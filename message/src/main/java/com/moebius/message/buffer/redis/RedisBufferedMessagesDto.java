package com.moebius.message.buffer.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class RedisBufferedMessagesDto {
    private String messageKey;
    private long firstReceivedMills;
    private String dedupStrategy;
    private long dedupPeriodMinutes;
    private String title;
    private String templateId;
    private Map<String, String> messageParameter;
    private String recipientType;
    private String recipientId;
}
