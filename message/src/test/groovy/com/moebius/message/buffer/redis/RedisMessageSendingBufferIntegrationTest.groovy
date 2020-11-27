package com.moebius.message.buffer.redis

import com.moebius.message.domain.*
import io.lettuce.core.ReadFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.test.context.ContextConfiguration
import reactor.test.StepVerifier
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Subject

import java.util.stream.Collectors

@SuppressWarnings(['GroovyAssignabilityCheck', 'GroovyAccessibility'])
@ContextConfiguration(classes = RedisTestConfig.class)
@Ignore
class RedisMessageSendingBufferIntegrationTest extends Specification {
    @Autowired
    ReactiveRedisOperations<String, RedisBufferedMessagesDto> redisOperations

    @Subject
    def sut

    def setup() {
        sut = new RedisMessageSendingBuffer(redisOperations)
        redisOperations.keys("*")
                .map({
                    redisOperations.delete(it)
                })
                .subscribe()
    }

    def "put messageSendRequest"() {
        given:
        messageInBuffer.forEach({
            sut.put(it.get(0), it.get(1))
        })
        def messageKey = "testMessageKey"
        def messageSendRequest = new MessageSendRequest(
                new DedupParameters(DedupStrategy.LEAVE_FIRST_ARRIVAL, 1), "testTitle",
                MessageBody.builder().templateId("test_template_id").parameters([:]).build(),
                Recipient.builder().recipientId("test_channel").recipientType(RecipientType.SLACK).build()
        )

        expect:
        StepVerifier.create(sut.put(messageKey, messageSendRequest))
                .assertNext({ it })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey1", createMsgRequest())],
                []
        ]
    }

    def "put messageSendRequest with wrong messageKey"() {
        given:
        def messageKey = ""
        def messageSendRequest = new MessageSendRequest(
                new DedupParameters(DedupStrategy.LEAVE_FIRST_ARRIVAL, 1), "testTitle",
                MessageBody.builder().templateId("test_template_id").parameters([:]).build(),
                Recipient.builder().recipientId("test_channel").recipientType(RecipientType.SLACK).build()
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
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey1", createMsgRequest())],
                [new Tuple("testMessageKey", createMsgRequest())],
                [new Tuple("testMessageKey1", createMsgRequest())],
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
                    def messageKeySet = it.stream().map({ buffered -> buffered.messageKey }).collect(Collectors.toSet())
                    messageKeys.forEach({ messageKey -> messageKeySet.contains(messageKey) })
                })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey1", createMsgRequest())],
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey", createMsgRequest())],
                [new Tuple("testMessageKey1", createMsgRequest())],
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
                .assertNext({ it == deleted })
                .verifyComplete()

        where:
        messageInBuffer << [
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey1", createMsgRequest())],
                [new Tuple("testMessageKey", createMsgRequest()), new Tuple("testMessageKey", createMsgRequest())],
                [new Tuple("testMessageKey1", createMsgRequest())],
                []
        ]
        bufferedMessageCount << [1, 0, 1, 0]
        deleted << [true, true, false, false]
    }

    MessageSendRequest createMsgRequest() {
        return MessageSendRequest.builder()
                .dedupParameters(DedupParameters.builder()
                        .dedupStrategy(DedupStrategy.LEAVE_LAST_ARRIVAL)
                        .dedupPeriodMinutes(1L)
                        .build())
                .body(MessageBody.builder()
                        .templateId("test_template_id")
                        .parameters([:])
                        .build())
                .recipient(Recipient.builder()
                        .recipientType(RecipientType.SLACK)
                        .recipientId("test_channel")
                        .build())
                .build()
    }

    @SuppressWarnings(['GrMethodMayBeStatic', 'unused', 'GrUnnecessaryPublicModifier'])
    @Configuration
    static class RedisTestConfig {
        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            def clientConfig = LettuceClientConfiguration.builder()
                    .readFrom(ReadFrom.REPLICA_PREFERRED)
                    .build()
            def serverConfig = new RedisStandaloneConfiguration("127.0.0.1", 6379)
            return new LettuceConnectionFactory(serverConfig, clientConfig)
        }

        @Bean
        public ReactiveRedisOperations<String, RedisBufferedMessagesDto> redisOperations(ReactiveRedisConnectionFactory factory) {
            Jackson2JsonRedisSerializer<RedisBufferedMessagesDto> serializer = new Jackson2JsonRedisSerializer<>(RedisBufferedMessagesDto.class)

            RedisSerializationContext.RedisSerializationContextBuilder<String, RedisBufferedMessagesDto> builder = RedisSerializationContext.newSerializationContext(new StringRedisSerializer())

            RedisSerializationContext<String, RedisBufferedMessagesDto> context = builder.value(serializer).build()

            return new ReactiveRedisTemplate<>(factory, context)
        }
    }


}
