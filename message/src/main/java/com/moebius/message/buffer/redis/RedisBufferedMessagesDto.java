package com.moebius.message.buffer.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public class RedisBufferedMessagesDto {
    private final String messageKey;
    private final LocalDateTime firstReceivedTime;
    private final String dedupStrategy;
    private final long dedupPeriodMinutes;
    private final String title;
    private final String templateId;
    private final Map<String, String> messageParameter;
    private final String recipientType;
    private final String recipientId;
}
