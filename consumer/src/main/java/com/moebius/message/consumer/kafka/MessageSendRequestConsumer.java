package com.moebius.message.consumer.kafka;

import com.moebius.message.MessageSendingController;
import com.moebius.message.consumer.assembler.MessageSendRequestAssembler;
import com.moebius.backend.dto.message.MessageSendRequestDto;
import groovy.util.logging.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Optional;

@Slf4j
@Component
public class MessageSendRequestConsumer extends KafkaConsumer<String, MessageSendRequestDto> {
    private static final String MESSAGE_SENDING_KAFKA_TOPIC = "moebius.message.send";
    private final MessageSendingController messageSendingController;
    private final MessageSendRequestAssembler requestAssembler;

    public MessageSendRequestConsumer(ReceiverOptions<?, ?> baseReceiverOptions,
                                      MessageSendingController controller,
                                      MessageSendRequestAssembler requestAssembler) {
        super(baseReceiverOptions);
        this.messageSendingController = controller;
        this.requestAssembler = requestAssembler;

    }

    @Override
    public String getTopic() {
        return MESSAGE_SENDING_KAFKA_TOPIC;
    }

    @Override
    public void processRecord(ReceiverRecord<String, MessageSendRequestDto> record) {
        ReceiverOffset offset = record.receiverOffset();
        Optional.of(record.value())
                .map(requestAssembler::assembleMessageSendRequest)
                .map(messageSendingController::receiveMessageSendRequest)
                .map(Mono::subscribe);

        offset.acknowledge();
    }


    @Override
    protected Class<?> getKeyDeserializerClass() {
        return StringDeserializer.class;
    }

    @Override
    protected Class<?> getValueDeserializerClass() {
        return JsonDeserializer.class;
    }
}
