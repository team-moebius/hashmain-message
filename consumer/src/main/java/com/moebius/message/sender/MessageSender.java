package com.moebius.message.sender;

import com.moebius.message.entity.MessageSendRequest;
import reactor.core.publisher.Mono;

public interface MessageSender {
    Mono<Boolean> sendMessage(MessageSendRequest messageSendRequest);
}
