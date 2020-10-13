package com.moebius.message.buffer

import com.moebius.message.domain.DedupStrategy
import com.moebius.message.domain.MessageBody
import com.moebius.message.domain.MessageSendRequest
import com.moebius.message.domain.Recipient
import reactor.test.StepVerifier
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Collectors

@SuppressWarnings(['GroovyAssignabilityCheck', 'GroovyAccessibility'])
class LocalMemoryMessageSendingBufferTest extends Specification {
    @Subject
    def sut = new LocalMemoryMessageSendingBuffer()

    def setup() {
        LocalMemoryMessageSendingBuffer.bufferedMessages.clear()
    }

    def "put messageSendRequest"() {
        given:
        messageInBuffer.forEach({
            sut.put(it.get(0), it.get(1))
        })
        def messageKey = "testMessageKey"
        def messageSendRequest = new MessageSendRequest(
                DedupStrategy.LEAVE_FIRST_ARRIVAL, "testTitle", Mock(MessageBody), Mock(Recipient)
        )

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
        def messageKey = ""
        def messageSendRequest = new MessageSendRequest(
                DedupStrategy.LEAVE_FIRST_ARRIVAL, "testTitle", Mock(MessageBody), Mock(Recipient)
        )

        expect:
        StepVerifier.create(sut.put(messageKey, messageSendRequest))
                .expectError(IllegalArgumentException)
                .verify()
    }

    def "check specific messageKey in buffer "() {
        given:
        messageInBuffer.forEach({ sut.put(it.get(0), it.get(1)) })
        def messageKey = "testMessageKey"

        expect:
        StepVerifier.create(sut.hasDuplicatedMessageWith(messageKey))
                .assertNext({ it == result })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", mockMessageSendRequest()), new Tuple("testMessageKey1", mockMessageSendRequest())],
                [new Tuple("testMessageKey", mockMessageSendRequest())],
                [new Tuple("testMessageKey1", mockMessageSendRequest())],
                []
        ]
        result << [true, true, false, false]
    }

    def "get all bufferedMessages"() {
        given:
        messageInBuffer.forEach({ sut.put(it.get(0), it.get(1)) })

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
        messageInBuffer.forEach({ sut.put(it.get(0), it.get(1)) })
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
        mockRequest.getDedupStrategy() >> DedupStrategy.LEAVE_FIRST_ARRIVAL
        return mockRequest
    }
}
