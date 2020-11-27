package com.moebius.message.consumer.configuration;

import com.moebius.message.buffer.redis.RedisBufferedMessagesDto;
import io.lettuce.core.ReadFrom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Profile({"prod", "local"})
@Configuration
public class RedisConfig {
    @Value("${redis.master-server}")
    private String masterServerUrl;
    @Value("${redis.master-port}")
    private Integer masterServerPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(masterServerUrl, masterServerPort);
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

