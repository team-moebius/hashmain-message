package com.moebius.message.buffer;

import com.moebius.message.domain.BufferedMessages;
import com.moebius.message.domain.MessageSendRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageSendingBuffer {
    Mono<Boolean> hasDuplicatedMessageWith(String messageKey);
    Mono<Boolean> put(String messageKey, MessageSendRequest messageSendRequest);
    Flux<BufferedMessages> getAllBufferedMessages();
    Mono<Boolean> dropKey(String messageKey);
}
