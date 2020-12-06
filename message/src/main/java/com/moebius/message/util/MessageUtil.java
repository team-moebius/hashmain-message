package com.moebius.message.util;

import com.moebius.message.domain.DedupParameters;
import com.moebius.message.domain.DedupStrategy;
import com.moebius.message.domain.MessageSendRequest;
import java.util.Optional;
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

        return Optional.of(messageSendRequest)
            .map(MessageSendRequest::getDedupParameters)
            .map(DedupParameters::getDedupStrategy)
            .map(dedupStrategy -> Mono.<Boolean>empty())
            .orElse(Mono.error(new IllegalArgumentException("DedupStrategy of MessageSendRequest must not be null")));
    }
}
