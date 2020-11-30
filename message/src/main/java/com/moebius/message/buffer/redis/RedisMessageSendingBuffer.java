package com.moebius.message.buffer.redis;

import com.moebius.message.buffer.MessageSendingBuffer;
import com.moebius.message.domain.*;
import com.moebius.message.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RedisMessageSendingBuffer implements MessageSendingBuffer {
    private static final String REDIS_KEY_FORMAT_FOR_MSG_BUFFER = "msg.buffer.%s";
    private final ReactiveRedisOperations<String, RedisBufferedMessagesDto> bufferOps;

    @Override
    public Mono<Boolean> hasDuplicatedMessageWith(String messageKey) {
        return bufferOps.hasKey(toRedisKey(messageKey));
    }

    @Override
    public Mono<Boolean> put(String messageKey, MessageSendRequest messageSendRequest) {
        return MessageUtil.validateRequestAndKey(messageKey, messageSendRequest)
                .switchIfEmpty(insertToBuffer(messageKey, messageSendRequest));
    }

    @Override
    public Flux<BufferedMessages> getAllBufferedMessages() {
        return bufferOps.keys(String.format(REDIS_KEY_FORMAT_FOR_MSG_BUFFER, "*"))
                .map(redisKey -> bufferOps.opsForList().range(redisKey, 0, -1))
                .flatMap(Flux::collectList)
                .filter(messages -> !messages.isEmpty())
                .map(this::fromRedisBufferedMessages);
    }

    @Override
    public Mono<Boolean> dropKey(String messageKey) {
        return bufferOps.delete(toRedisKey(messageKey))
                .map(deletedSize -> deletedSize > 0);
    }

    private Mono<Boolean> insertToBuffer(String messageKey, MessageSendRequest messageSendRequest) {
        return bufferOps.opsForList().rightPush(toRedisKey(messageKey), toRedisBufferDto(messageKey, messageSendRequest))
                .map(index -> index > 0);
    }

    private RedisBufferedMessagesDto toRedisBufferDto(String messageKey, MessageSendRequest messageSendRequest) {

        return new RedisBufferedMessagesDto(
                messageKey, System.currentTimeMillis(),
                messageSendRequest.getDedupParameters().getDedupStrategy().name(),
                messageSendRequest.getDedupParameters().getDedupPeriodMinutes(),
                messageSendRequest.getTitle(),
                messageSendRequest.getBody().getTemplateId(), messageSendRequest.getBody().getParameters(),
                messageSendRequest.getRecipient().getRecipientType().name(),
                messageSendRequest.getRecipient().getRecipientId()
        );
    }
    private BufferedMessages fromRedisBufferedMessages(List<RedisBufferedMessagesDto> bufferedMessages){
        RedisBufferedMessagesDto firstReceivedMessage = bufferedMessages.get(0);

        LocalDateTime firstReceivedTime = Instant.ofEpochMilli(firstReceivedMessage.getFirstReceivedMills())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        String messageKey = firstReceivedMessage.getMessageKey();

        DedupStrategy dedupStrategy = EnumUtils.getEnum(DedupStrategy.class, firstReceivedMessage.getDedupStrategy(), DedupStrategy.NO_DEDUP);
        long dedupPeriodMinutes = firstReceivedMessage.getDedupPeriodMinutes();
        DedupParameters dedupParameters = DedupParameters.builder()
                .dedupStrategy(dedupStrategy)
                .dedupPeriodMinutes(dedupPeriodMinutes)
                .build();

        List<MessageSendRequest> bufferedRequests = bufferedMessages.stream().map(redisBuffer -> new MessageSendRequest(
                dedupParameters, redisBuffer.getTitle(),
                MessageBody.builder().templateId(redisBuffer.getTemplateId()).parameters(redisBuffer.getMessageParameter()).build(),
                Recipient.builder()
                        .recipientId(redisBuffer.getRecipientId())
                        .recipientType(EnumUtils.getEnum(RecipientType.class, firstReceivedMessage.getRecipientType())).build()
        )).collect(Collectors.toList());

        return new BufferedMessages(
                dedupParameters.getDedupStrategy(),
                dedupParameters.getDedupPeriodMinutes(),
                firstReceivedTime,
                bufferedRequests, messageKey
        );
    }

    protected String toRedisKey(String messageKey) {
        return String.format(REDIS_KEY_FORMAT_FOR_MSG_BUFFER, messageKey);
    }
}
