package com.moebius.message

import com.moebius.message.buffer.MessageSendingBuffer
import com.moebius.message.dedup.DedupStrategy
import com.moebius.message.entity.MessageBody
import com.moebius.message.entity.MessageSendRequest
import com.moebius.message.entity.Recipient
import com.moebius.message.entity.RecipientType
import com.moebius.message.keygen.MessageKeyGenerator
import com.moebius.message.sender.MessageSender
import com.moebius.message.sender.MessageSenderResolver
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll
import reactor.test.StepVerifier

class MessageSendingControllerTest extends Specification {
    def messageSenderResolver = Mock(MessageSenderResolver)
    def messageSender = Mock(MessageSender)
    def messageSendingBuffer = Mock(MessageSendingBuffer)
    def messageKeyGenerator = Mock(MessageKeyGenerator)

    def testMessageKey = "TEST-MESSAGE-KEY"

    @Subject
    def sut = new MessageSendingController(messageKeyGenerator, messageSendingBuffer, messageSenderResolver)

    def setup() {
        messageSenderResolver.getSender(_ as RecipientType) >> messageSender
        messageKeyGenerator.generateMessageKey(_ as MessageSendRequest) >> testMessageKey
    }

    @Unroll
    def "Test message sending for each conditions #condition"() {
        given:
        def recipient = new Recipient(RecipientType.SLACK, "test-id")
        def messageSendRequest = MessageSendRequest.builder()
                .dedupStrategy(dedupStrategy)
                .title("test title")
                .body(Mock(MessageBody))
                .recipient(recipient)
                .build()

        if (DedupStrategy.LEAVE_FIRST_ARRIVAL == dedupStrategy){
            1 * messageSendingBuffer.hasDuplicatedMessageWith(testMessageKey) >> Mono.just(duplicatedMessageOnBuffer)
        }

        if (shouldSend) {
            1 * messageSender.sendMessage(messageSendRequest) >> Mono.just(true)
        } else {
            0 * messageSender.sendMessage(messageSendRequest)
        }

        if (shouldSaveToBuffer) {
            1 * messageSendingBuffer.put(testMessageKey, messageSendRequest) >> Mono.just(true)
        } else {
            0 * messageSendingBuffer.put(_ as String, _ as MessageSendRequest)
        }

        expect:
        StepVerifier.create(sut.receiveMessageSendRequest(messageSendRequest))
                .assertNext({
                    it.sent == shouldSend
                    it.msgSavedToBuffer == shouldSaveToBuffer
                    it.result
                })
                .verifyComplete()
        where:
        dedupStrategy                     | duplicatedMessageOnBuffer | shouldSend | shouldSaveToBuffer | condition
        DedupStrategy.NO_DEDUP            | false                     | true       | false              | "send with no deduplication"
        DedupStrategy.LEAVE_FIRST_ARRIVAL | false                     | true       | true               | "send only first arrival message"
        DedupStrategy.LEAVE_FIRST_ARRIVAL | true                      | false      | true               | "ignore messages after sent first arrival one"
        DedupStrategy.LEAVE_LAST_ARRIVAL  | false                     | false      | true               | "not send message for deduplicate later"
        DedupStrategy.LEAVE_LAST_ARRIVAL  | true                      | false      | true               | "not send message for deduplicate later for another one"
    }
}
