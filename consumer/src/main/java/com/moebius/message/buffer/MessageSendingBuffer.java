package com.moebius.message.buffer;

import com.moebius.message.entity.MessageSendRequest;
import reactor.core.publisher.Mono;

public interface MessageSendingBuffer {
    Mono<Boolean> hasDuplicatedMessageWith(String messageKey);
    Mono<Boolean> put(String messageKey, MessageSendRequest messageSendRequest);
}
