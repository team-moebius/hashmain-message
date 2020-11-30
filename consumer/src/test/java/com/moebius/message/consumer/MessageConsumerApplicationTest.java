package com.moebius.message.consumer;

import com.moebius.backend.dto.message.MessageBodyDto;
import com.moebius.backend.dto.message.MessageSendRequestDto;
import com.moebius.message.BufferedMessageSendingController;
import com.moebius.message.MessageSendingController;
import com.moebius.message.buffer.redis.RedisBufferedMessagesDto;
import com.moebius.message.domain.DedupStrategy;
import io.lettuce.core.ReadFrom;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("it")
@Ignore
public class MessageConsumerApplicationTest {
    @SpyBean
    private MessageSendingController messageSendingController;

    @SpyBean
    private BufferedMessageSendingController bufferedMessageSendingController;

    @Autowired
    private ReactiveRedisOperations<String, RedisBufferedMessagesDto> bufferOps;

    public static String MESSAGE_TOPIC = "moebius.message.send";
    private static final String TEST_GROUP_ID = "testGroup";
    private static final String AUTO_COMMIT = "false";

    @Test
    public void contextLoads() {
    }

    @Test
    public void messageSendTest() throws InterruptedException {
        StepVerifier.create(bufferOps.keys("*").flatMap(key -> bufferOps.delete(key)))
                .thenConsumeWhile(senderResultFlux -> true)
                .expectComplete()
                .verify();
        Map<String, String> messageParam = getMessageParam();
        List<MessageSendRequestDto> requestDtoList = Arrays.stream(new DedupStrategy[]{
                DedupStrategy.NO_DEDUP, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_LAST_ARRIVAL,
                DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL,
                DedupStrategy.LEAVE_LAST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL,
                DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL, DedupStrategy.LEAVE_FIRST_ARRIVAL
        }).map(dedupStrategy -> new MessageSendRequestDto(
                dedupStrategy.name(), 1, "title-for-" + dedupStrategy.name(),
                new MessageBodyDto("test_template_id", messageParam),
                "SLACK", "messaging-test"
        )).collect(Collectors.toList());

        KafkaSender<String, MessageSendRequestDto> kafkaSender = KafkaSender.create(senderOptions());

        StepVerifier.create(Flux.zip(Flux.fromIterable(requestDtoList), Flux.range(0, requestDtoList.size()))
                .delayElements(Duration.ofMillis(500))
                .map(requestDtoIndexPair -> {
                    MessageSendRequestDto requestDto = requestDtoIndexPair.getT1();
                    int index = requestDtoIndexPair.getT2();
                    String key = StringUtils.joinWith("-", requestDto.getDedupStrategy(), requestDto.getRecipientType(), requestDto.getTitle());
                    return Mono.just(SenderRecord.create(new ProducerRecord<>("moebius.message.send", key, requestDto), key+index));
                })
                .flatMap(kafkaSender::send))
                .thenConsumeWhile(senderResultFlux -> true)
                .expectComplete()
                .verify();

        kafkaSender.close();


        Thread.sleep(90000);

        verify(messageSendingController, times(requestDtoList.size())).receiveMessageSendRequest(any());
        verify(bufferedMessageSendingController, atLeast(1)).sendBufferedMessagesBefore(any());
    }

    private SenderOptions<String, MessageSendRequestDto> senderOptions() {
        Map<String, Object> senderDefaultProperties = new HashMap<>();
        senderDefaultProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", "localhost:9092"));
        senderDefaultProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "moebius-consumer");
        senderDefaultProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        senderDefaultProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.moebius.message.consumer.dto");
        senderDefaultProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        senderDefaultProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return SenderOptions.create(senderDefaultProperties);
    }

    private Map<String, String> getMessageParam() {
        Map<String, String> messageParamMap = new HashMap<>();
        messageParamMap.put("color", "#0051C7");
        messageParamMap.put("author", "test-sender");
        messageParamMap.put("authorLink", "http,//test-message.com");

        messageParamMap.put("tradeSymbol", "BTC");
        messageParamMap.put("tradeCreatedAt", "17,30,24");
        messageParamMap.put("tradeVolume", "123");
        messageParamMap.put("tradeCurrency", "BTC");

        messageParamMap.put("bidVolume", "456");
        messageParamMap.put("bidCurrency", "BTC");
        messageParamMap.put("tradePrice", "13,927,348");

        messageParamMap.put("priceChange", "-123,123");
        messageParamMap.put("unitCurrency", "KRW");
        messageParamMap.put("priceChangeRatio", "-23");

        return messageParamMap;
    }

    @TestConfiguration
    public static class TestKafkaConsumerConfig {
        @Bean
        public ReceiverOptions<?, ?> baseReceiverOptions() {
            Map<String, Object> receiverDefaultProperties = new HashMap<>();
            receiverDefaultProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", "localhost:9092"));
            receiverDefaultProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, "moebius-consumer");
            receiverDefaultProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "moebius-consumer");
            receiverDefaultProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
            receiverDefaultProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.moebius.message.consumer.dto, com.moebius.backend.dto.message");

            return ReceiverOptions.create(receiverDefaultProperties);
        }

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .readFrom(ReadFrom.REPLICA_PREFERRED)
                    .build();
            RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("localhost", 6379);
            return new LettuceConnectionFactory(serverConfig, clientConfig);
        }

        @Bean
        public ReactiveRedisOperations<String, RedisBufferedMessagesDto> redisOperations(ReactiveRedisConnectionFactory factory) {
            Jackson2JsonRedisSerializer<RedisBufferedMessagesDto> serializer = new Jackson2JsonRedisSerializer<>(RedisBufferedMessagesDto.class);

            RedisSerializationContext.RedisSerializationContextBuilder<String, RedisBufferedMessagesDto> builder
                    = RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

            RedisSerializationContext<String, RedisBufferedMessagesDto> context = builder.value(serializer).build();

            return new ReactiveRedisTemplate<>(factory, context);
        }
    }
}
