package com.moebius.message.keygen;


import com.moebius.message.domain.MessageSendRequest;

public interface MessageKeyGenerator {
    String generateMessageKey(MessageSendRequest messageSendRequest);
}
