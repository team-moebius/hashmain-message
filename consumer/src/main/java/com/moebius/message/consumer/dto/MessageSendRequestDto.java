package com.moebius.message.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageSendRequestDto {
    private final String dedupStrategy;
    private final long dedupPeriodMinutes;
    private final String title;
    private final MessageBodyDto body;
    private final String recipientType;
    private final String recipientId;
}
