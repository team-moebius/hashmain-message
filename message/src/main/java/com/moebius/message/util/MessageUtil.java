package com.moebius.message.util;

import com.moebius.message.domain.MessageSendRequest;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class MessageUtil {
    public static Mono<Boolean> validateRequestAndKey(String messageKey, MessageSendRequest messageSendRequest){
        if (StringUtils.isEmpty(messageKey)){
            return Mono.error(new IllegalArgumentException("Message Key must not be empty"));
        }

        if (Objects.isNull(messageSendRequest)){
            return Mono.error(new IllegalArgumentException("MessageSendRequest must not be null"));
        }

        if (Objects.isNull(messageSendRequest.getDedupParameters().getDedupStrategy())){
            return Mono.error(new IllegalArgumentException("DedupStrategy of MessageSendRequest must not be null"));
        }
        return Mono.empty();
    }
}
