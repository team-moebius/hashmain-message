package com.moebius.message.consumer.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class KafkaConsumer<K, V> {
	private final KafkaReceiver<K, V> receiver;

	public KafkaConsumer(Map<String, String> receiverDefaultProperties) {
		Map<String, Object> properties = new HashMap<>(receiverDefaultProperties);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getKeyDeserializerClass());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getValueDeserializerClass());

		ReceiverOptions<K, V> receiverOptions = ReceiverOptions.create(properties);
		receiverOptions.subscription(Collections.singleton(getTopic()))
			.addAssignListener(partitions -> log.debug("[Kafka] onPartitionsAssigned {}", partitions))
			.addRevokeListener(partitions -> log.debug("[Kafka] onPartitionsRevoked {}", partitions));

		receiver = KafkaReceiver.create(receiverOptions);
	}

	public abstract String getTopic();

	public abstract void processRecord(ReceiverRecord<K, V> record);

	protected abstract Class<?> getKeyDeserializerClass();

	protected abstract Class<?> getValueDeserializerClass();

	public void consumeMessages() {
		log.info("[Kafka] Start to read messages. [{}]", getTopic());
		receiver.receive()
			.publishOn(Schedulers.elastic())
			.groupBy(ConsumerRecord::key)
			.flatMap(groupedFlux -> groupedFlux.sampleFirst(Duration.ofSeconds(3)))
			.subscribe(this::processRecord);
	}
}