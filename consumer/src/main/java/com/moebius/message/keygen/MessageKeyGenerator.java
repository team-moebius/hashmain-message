package com.moebius.message.keygen;


import com.moebius.message.entity.MessageSendRequest;

public interface MessageKeyGenerator {
    String generateMessageKey(MessageSendRequest messageSendRequest);
}
