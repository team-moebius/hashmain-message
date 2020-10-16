package com.moebius.message

import com.moebius.message.buffer.MessageSendingBuffer
import com.moebius.message.domain.*
import com.moebius.message.sender.MessageSender
import com.moebius.message.sender.MessageSenderResolver
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime
import java.util.stream.Collectors
import java.util.stream.IntStream

class BufferedMessageSendingControllerTest extends Specification {
    def messageSenderResolver = Mock(MessageSenderResolver)
    def messageSender = Mock(MessageSender)
    def messageSendingBuffer = Mock(MessageSendingBuffer)

    def deadLineToSendMessage = LocalDateTime.of(2020, 10, 12, 22, 25)
    def dedupPeriodInMinutes = 1L

    @Subject
    def sut = new BufferedMessageSendingController(messageSendingBuffer, messageSenderResolver)

    def setup() {
        messageSenderResolver.getSender(_ as RecipientType) >> messageSender
    }

    def "Test not sending when dedup strategy of messages saved on buffer are not LEAVE_LAST_ARRIVAL"() {
        given:
        def messagesInBuffer = [
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 5, 10),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 10, 5),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 15, 3),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 3, 2),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 21, 0),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 21, 1),
        ]
        1 * messageSendingBuffer.getAllBufferedMessages() >> Flux.fromIterable(messagesInBuffer)
        0 * messageSender.sendMessage(_ as MessageSendRequest)
        6 * messageSendingBuffer.dropKey(_ as String) >> Mono.just(true)

        expect:
        StepVerifier.create(sut.sendBufferedMessagesBefore(deadLineToSendMessage))
                .verifyComplete()
    }

    def "Test not sending when all messages on buffer are saved before deadline"() {
        given:
        def messagesInBuffer = [
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 5, -10),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 10, -5),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 15, -3),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 3, -2),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 21, -1),
        ]
        1 * messageSendingBuffer.getAllBufferedMessages() >> Flux.fromIterable(messagesInBuffer)
        0 * messageSender.sendMessage(_ as MessageSendRequest)
        0 * messageSendingBuffer.dropKey(_ as String)

        expect:
        StepVerifier.create(sut.sendBufferedMessagesBefore(deadLineToSendMessage))
                .verifyComplete()
    }

    def "Test sending messages when all messages on buffer are saved after deadline and has dedup strategy of LEAVE_LAST_ARRIVAL"() {
        given:
        def messagesInBuffer = [
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 5, 10),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 10, 5),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 15, 3),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 3, 2),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 21, 4),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 21, 0),
        ]
        1 * messageSendingBuffer.getAllBufferedMessages() >> Flux.fromIterable(messagesInBuffer)
        6 * messageSender.sendMessage({
            it.dedupParameters.dedupStrategy == DedupStrategy.LEAVE_LAST_ARRIVAL
        }) >> Mono.just(true)
        6 * messageSendingBuffer.dropKey(_ as String) >> Mono.just(true)

        expect:
        StepVerifier.create(sut.sendBufferedMessagesBefore(deadLineToSendMessage))
                .recordWith({ return [] })
                .thenConsumeWhile({ true })
                .consumeRecordedWith({
                    assert it.size() == 6
                    it.every { sendingResult ->
                        assert sendingResult.sent
                        assert !sendingResult.msgSavedToBuffer
                        assert sendingResult.result
                        true
                    }
                })
                .verifyComplete()
    }

    def "Test sending messages only with LEAVE_LAST_ARRIVAL and saved before deadline saved time and dedup strategy are mixed on buffer"() {
        given:
        def messagesInBuffer = [
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 5, 10),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 10, 5),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 15, 3),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 3, 2),
                createBufferedMessages(DedupStrategy.LEAVE_FIRST_ARRIVAL, 21, 4),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 5, -10),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 10, -5),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 15, -3),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 3, -2),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 21, -1),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 5, 10),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 10, 5),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 15, 3),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 3, 2),
                createBufferedMessages(DedupStrategy.LEAVE_LAST_ARRIVAL, 21, 4),
        ]
        1 * messageSendingBuffer.getAllBufferedMessages() >> Flux.fromIterable(messagesInBuffer)
        5 * messageSender.sendMessage({
            it.dedupParameters.dedupStrategy == DedupStrategy.LEAVE_LAST_ARRIVAL
        }) >> Mono.just(true)
        10 * messageSendingBuffer.dropKey(_ as String) >> Mono.just(true)

        expect:
        StepVerifier.create(sut.sendBufferedMessagesBefore(deadLineToSendMessage))
                .recordWith({ return [] })
                .thenConsumeWhile({ true })
                .consumeRecordedWith({
                    it.every {
                        { sendingResult ->
                            sendingResult.sent && sendingResult.msgSavedToBuffer == false && sendingResult.result
                        }
                    }
                })
                .verifyComplete()
    }

    BufferedMessages createBufferedMessages(DedupStrategy dedupStrategy, int msgCount, int minutesToAddDeadLine) {
        def mockRecipient = new Recipient(RecipientType.SLACK, "")
        def firstReceivedTime = deadLineToSendMessage.plusMinutes(minutesToAddDeadLine)
        def messageKey = "${dedupStrategy}-${msgCount}-${minutesToAddDeadLine}"

        def msgRequests = IntStream.range(0, msgCount).mapToObj({
            new MessageSendRequest(
                    DedupParameters.builder().dedupStrategy(dedupStrategy).dedupPeriodMinutes(dedupPeriodInMinutes).build(),
                    "title-${it}", Mock(MessageBody), mockRecipient
            )
        }).collect(Collectors.toList())

        return new BufferedMessages(dedupStrategy, dedupPeriodInMinutes, firstReceivedTime, msgRequests, messageKey)
    }
}
