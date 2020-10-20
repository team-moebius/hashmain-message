package com.moebius.message.sender;

import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.domain.RecipientType;
import reactor.core.publisher.Mono;

public interface MessageSender {
    Mono<Boolean> sendMessage(MessageSendRequest messageSendRequest);
    RecipientType getRecipientType();
}
