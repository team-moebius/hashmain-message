package com.moebius.message.consumer.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.util.Collections;
import java.util.Map;

@Slf4j
public abstract class KafkaConsumer<K, V> {
    private final ReceiverOptions<K, V> receiverOptions;
    private KafkaReceiver<K, V> receiver;
    private Disposable receiverDisposable;

    public KafkaConsumer(ReceiverOptions<?, ?> baseReceiverOptions) {
        Map<String, Object> consumerProperties = baseReceiverOptions.consumerProperties();
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getKeyDeserializerClass());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getValueDeserializerClass());

        receiverOptions = ReceiverOptions.create(consumerProperties);
        receiverOptions.subscription(Collections.singleton(getTopic()))
                .addAssignListener(partitions -> log.debug("[Kafka] onPartitionsAssigned {}", partitions))
                .addRevokeListener(partitions -> log.debug("[Kafka] onPartitionsRevoked {}", partitions));
    }

    public abstract String getTopic();

    public abstract void processRecord(ReceiverRecord<K, V> record);

    protected abstract Class<?> getKeyDeserializerClass();

    protected abstract Class<?> getValueDeserializerClass();

    public void consumeMessages() {
        log.info("[Kafka] Start to read messages. [{}]", getTopic());
        if (receiverDisposable != null && !receiverDisposable.isDisposed()) {
            receiverDisposable.dispose();
        }
        receiver = KafkaReceiver.create(receiverOptions);
        receiverDisposable = receiver.receive()
                .publishOn(Schedulers.elastic())
                .doOnTerminate(this::consumeMessages)
                .subscribe(this::processRecord);
    }
}