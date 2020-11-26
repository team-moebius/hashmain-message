package com.moebius.message.buffer.redis

import com.moebius.message.domain.DedupParameters
import com.moebius.message.domain.DedupStrategy
import com.moebius.message.domain.MessageBody
import com.moebius.message.domain.MessageSendRequest
import com.moebius.message.domain.Recipient
import com.moebius.message.domain.RecipientType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveListOperations

import java.time.LocalDateTime
import java.util.stream.Collectors

@SuppressWarnings(['GroovyAssignabilityCheck', 'GroovyAccessibility'])
class RedisMessageSendingBufferTest extends Specification {
    ReactiveRedisOperations<String, RedisBufferedMessagesDto> mockRedisOps = Mock(ReactiveRedisOperations)
    ReactiveListOperations<String, RedisBufferedMessagesDto> mockListOps = Mock(ReactiveListOperations)
    @Subject
    def sut = new RedisMessageSendingBuffer(mockRedisOps)

    def setup(){
        mockRedisOps.opsForList() >> mockListOps
        mockRedisOps.delete(_ as String) >> Mono.just(1L)
    }

    def "put messageSendRequest"() {
        given:
        def messageKey = "testMessageKey"
        def messageSendRequest = new MessageSendRequest(
                new DedupParameters(DedupStrategy.LEAVE_FIRST_ARRIVAL, 1), "testTitle", Mock(MessageBody),
                Recipient.builder().recipientId("test_channel").recipientType(RecipientType.SLACK).build()
        )
        1 * mockListOps.rightPush(
                {it.contains(messageKey)},
                {it.messageKey == messageKey \
                    && it.dedupStrategy == DedupStrategy.LEAVE_FIRST_ARRIVAL.name() \
                    && it.dedupPeriodMinutes == 1
                }
        ) >> Mono.just(1L)

        expect:
        StepVerifier.create(sut.put(messageKey, messageSendRequest))
                .assertNext({ it })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey1", mockMessageSendRequest())],
                []
        ]
    }

    def "put messageSendRequest with wrong messageKey"() {
        given:
        mockListOps.rightPush(_ as String, _ as RedisBufferedMessagesDto) >> Mono.just(0)
        def messageKey = ""
        def messageSendRequest = new MessageSendRequest(
                new DedupParameters(DedupStrategy.LEAVE_FIRST_ARRIVAL, 1), "testTitle", Mock(MessageBody),
                Recipient.builder().recipientId("test_channel").recipientType(RecipientType.SLACK).build()
        )

        expect:
        StepVerifier.create(sut.put(messageKey, messageSendRequest))
                .expectError(IllegalArgumentException)
                .verify()
    }

    def "check specific messageKey in buffer "() {
        given:
        mockRedisOps.hasKey(_ as String) >> Mono.just(false)
        keysInBuffer.forEach({mockRedisOps.hasKey(sut.toRedisKey(it)) >> Mono.just(true)})
        def messageKey = "testMessageKey"

        expect:
        StepVerifier.create(sut.hasDuplicatedMessageWith(messageKey))
                .assertNext({ it == result })
                .verifyComplete()

        where:
        keysInBuffer << [
                ["testMessageKey", "testMessageKey1"],
                ["testMessageKey"],
                ["testMessageKey1"],
                []
        ]
        result << [true, true, false, false]
    }

    def "get all bufferedMessages"() {
        given:
        def allMessages = messageInBuffer.stream()
                .map({
                    new RedisBufferedMessagesDto(
                            it.first(), LocalDateTime.now(), DedupStrategy.LEAVE_LAST_ARRIVAL.name(), 1,
                            it.first(), "test_tempate", [:], RecipientType.SLACK.name(), "test_channel"
                    )
                })
                .collect(Collectors.groupingBy({RedisBufferedMessagesDto it-> sut.toRedisKey(it.messageKey)}))
        mockRedisOps.keys(_ as String) >> Flux.fromIterable(allMessages.keySet().toList())

        allMessages.forEach({String redisKey, messages->
            mockListOps.range(redisKey, _ as long, _ as long) >> Flux.fromIterable(messages)
        })


        expect:
        StepVerifier.create(sut.getAllBufferedMessages())
                .recordWith({ return [] })
                .thenConsumeWhile({ true })
                .consumeRecordedWith({
                    it.size() == bufferedMessageCount
                    def messageKeySet = it.stream().map({buffered->buffered.messageKey}).collect(Collectors.toSet())
                    messageKeys.forEach({messageKey-> messageKeySet.contains(messageKey)})
                })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey1", mockMessageSendRequest())],
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey", mockMessageSendRequest())],
                [new Tuple("testMessageKey1", mockMessageSendRequest())],
                []
        ]
        bufferedMessageCount << [2, 1, 1, 0]
        messageKeys << [
                ["testMessageKey", "testMessageKey1"], ["testMessageKey"], ["testMessageKey1"], []
        ]
    }

    def "drop messageKey"() {
        given:
        mockRedisOps.delete(_ as String) >> Mono.just(0L)
        messageInBuffer.forEach({ mockRedisOps.delete(it.get(0)) >> Mono.just(1L) })
        def messageKey = "testMessageKey"

        expect:
        StepVerifier.create(sut.dropKey(messageKey))
                .assertNext({it == deleted})
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey1", mockMessageSendRequest())],
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey", mockMessageSendRequest())],
                [new Tuple("testMessageKey1", mockMessageSendRequest())],
                []
        ]
        bufferedMessageCount << [1, 0, 1, 0]
        deleted << [true, true, false, false]
    }

    MessageSendRequest mockMessageSendRequest() {
        def mockRequest = Mock(MessageSendRequest)
        def mockDedupParam = Mock(DedupParameters)
        mockDedupParam.getDedupStrategy() >> DedupStrategy.LEAVE_FIRST_ARRIVAL
        mockRequest.getDedupParameters() >> mockDedupParam
        def mockBody = Mock(MessageBody)
        mockBody.getTemplateId() >> "test_template_id"
        mockBody.getParameters() >> [:]
        mockRequest.getBody() >> mockBody
        def mockRecipient = Mock(Recipient)
        mockRecipient.getRecipientType() >> RecipientType.SLACK
        mockRequest.getRecipient() >> mockRecipient
        return mockRequest
    }

}
