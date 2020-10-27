package com.moebius.message.consumer.kafka

import com.moebius.message.MessageSendingController
import com.moebius.message.consumer.assembler.MessageSendRequestAssembler
import com.moebius.message.consumer.dto.MessageBodyDto
import com.moebius.message.consumer.dto.MessageSendRequestDto
import reactor.kafka.receiver.ReceiverOffset
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import spock.lang.Specification
import spock.lang.Subject

class MessageSendRequestConsumerTest extends Specification {
    def sendController = Mock(MessageSendingController)
    def assembler = new MessageSendRequestAssembler()
    def receiverOptions = ReceiverOptions.create([:])

    @Subject
    def sut = new MessageSendRequestConsumer(receiverOptions, sendController, assembler)

    def "Receive request from Kafka and request send"(){
        given:
        def requestDto = new MessageSendRequestDto(
                dedupStrategy, dedupPeriodMinute, title, new MessageBodyDto(templateId, [:]),
                recipientType, recipientId
        )
        def record = Mock(ReceiverRecord)
        def receiverOffset = Mock(ReceiverOffset)
        record.receiverOffset() >> receiverOffset
        record.value() >> requestDto

        when:
        sut.processRecord(record)

        then:
        1 * sendController.receiveMessageSendRequest({
            it.dedupParameters.dedupStrategy.name() == dedupStrategy \
            && it.dedupParameters.dedupPeriodMinutes == dedupPeriodMinute \
            && it.title == title \
            && it.body.templateId == templateId \
            && it.recipient.recipientType.name() == recipientType \
            && it.recipient.recipientId == recipientId
        })
        1 * receiverOffset.acknowledge()

        where:
        dedupStrategy   |   dedupPeriodMinute   |   title           |   templateId      |   recipientType   |   recipientId
        "NO_DEDUP"      |   5                   |   "test_title"    |   "test_template" |   "SLACK"         |   "test_recipient"

    }
}
