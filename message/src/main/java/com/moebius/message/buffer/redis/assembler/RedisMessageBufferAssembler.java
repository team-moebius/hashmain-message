package com.moebius.message.buffer.redis.assembler;

import com.moebius.message.buffer.redis.RedisBufferedMessagesDto;
import com.moebius.message.domain.BufferedMessages;
import com.moebius.message.domain.DedupParameters;
import com.moebius.message.domain.DedupStrategy;
import com.moebius.message.domain.MessageBody;
import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.domain.Recipient;
import com.moebius.message.domain.RecipientType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageBufferAssembler {
    public RedisBufferedMessagesDto toRedisBuffer(String messageKey, MessageSendRequest messageSendRequest){
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

    public BufferedMessages toBufferedMessage(List<RedisBufferedMessagesDto> bufferedMessages){
        RedisBufferedMessagesDto firstReceivedMessage = bufferedMessages.get(0);

        LocalDateTime firstReceivedTime = Instant.ofEpochMilli(firstReceivedMessage.getFirstReceivedMills())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        String messageKey = firstReceivedMessage.getMessageKey();

        DedupStrategy dedupStrategy = EnumUtils
            .getEnum(DedupStrategy.class, firstReceivedMessage.getDedupStrategy(), DedupStrategy.NO_DEDUP);
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
}
