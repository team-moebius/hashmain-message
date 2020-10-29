package com.moebius.message

import com.moebius.message.buffer.LocalMemoryMessageSendingBuffer
import com.moebius.message.domain.*
import com.moebius.message.keygen.MessageKeyGeneratorImpl
import com.moebius.message.sender.MessageSender
import com.moebius.message.sender.MessageSenderResolver
import com.moebius.message.sender.slack.SlackMessageBuilder
import com.moebius.message.sender.slack.SlackMessageSender
import com.moebius.message.sender.slack.template.FileBasedSlackMessageTemplateResolver
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.LocalDateTime

@Ignore
class MessageSendingIntegrationTest extends Specification {
    def messageSendingBuffer = new LocalMemoryMessageSendingBuffer()
    def messageKeyGenerator = new MessageKeyGeneratorImpl()

    def webClient = WebClient.create()
    def slackMessageBuilder = new SlackMessageBuilder(new FileBasedSlackMessageTemplateResolver())

    def slackMessageSenderForBufferedSender = Spy(
            SlackMessageSender, constructorArgs: [webClient, slackMessageBuilder]
    ) as MessageSender
    def messageSenderResolverForBufferedSender = new MessageSenderResolver(
            [slackMessageSenderForBufferedSender] as List<MessageSender>
    )

    def bufferedMessageSendingController = new BufferedMessageSendingController(messageSendingBuffer, messageSenderResolverForBufferedSender)

    def slackMessageSenderForSender = Spy(
            SlackMessageSender, constructorArgs: [webClient, slackMessageBuilder]
    ) as MessageSender
    def messageSenderResolverForSender = new MessageSenderResolver(
            [slackMessageSenderForSender] as List<MessageSender>
    )
    def messageSendingController = new MessageSendingController(
            messageKeyGenerator, messageSendingBuffer, messageSenderResolverForSender
    )

    def messageParam = [
            "color"      : "#0051C7", "author": "test-sender", "authorLink": "http://test-message.com",
            "tradeSymbol": "BTC", "tradeCreatedAt": "17:30:24", "tradeVolume": "123", "tradeCurrency": "BTC",
            "bidVolume"  : "456", "bidCurrency": "BTC", "tradePrice": "13,927,348",
            "priceChange": "-123,123", "unitCurrency": "KRW", "priceChangeRatio": "-23",
    ]

    def setup() {
        LocalMemoryMessageSendingBuffer.bufferedMessages.clear()
    }

    def "Message from original request and saved on buffer sending test"() {
        given:
        def dedupStrategySequences = [
                DedupStrategy.NO_DEDUP, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_LAST_ARRIVAL,
                DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL,
                DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL,
                DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL
        ]
        def messageSendFlux = Flux.interval(Duration.ofMillis(500)).take(dedupStrategySequences.size())
                .zipWith(Flux.fromIterable(dedupStrategySequences))
                .map({
                    it.mapT2({ t2 -> createMessageSendRequest(t2, "title-for-${t2.name()}") })
                })
                .flatMap({
                    messageSendingController.receiveMessageSendRequest(it.getT2())
                            .map({ result -> it })
                })
                .flatMap({
                    bufferedMessageSendingController.sendBufferedMessagesBefore(LocalDateTime.now())
                            .collectList()
                }).concatWith(bufferedMessageSendingController.sendBufferedMessagesBefore(LocalDateTime.now().plusMinutes(2)).collectList())

        1 * slackMessageSenderForBufferedSender.sendMessage({
            it.dedupParameters.dedupStrategy == DedupStrategy.LEAVE_LAST_ARRIVAL
        })
        1 * slackMessageSenderForSender.sendMessage({
            it.dedupParameters.dedupStrategy == DedupStrategy.LEAVE_FIRST_ARRIVAL
        })
        1 * slackMessageSenderForSender.sendMessage({
            it.dedupParameters.dedupStrategy == DedupStrategy.NO_DEDUP
        })

        expect:
        StepVerifier.create(messageSendFlux)
                .expectNextCount(dedupStrategySequences.size()+1)
                .verifyComplete()

    }

    MessageSendRequest createMessageSendRequest(DedupStrategy dedupStrategy, String title) {
        MessageSendRequest request = MessageSendRequest.builder()
                .dedupParameters(DedupParameters.builder()
                        .dedupStrategy(dedupStrategy)
                        .dedupPeriodMinutes(1L)
                        .build())
                .title(title)
                .body(MessageBody.builder()
                        .templateId("test_template_id")
                        .parameters(messageParam)
                        .build())
                .recipient(Recipient.builder()
                        .recipientId("messaging-test")
                        .recipientType(RecipientType.SLACK)
                        .build())
                .build()

        return request
    }
}
