package com.moebius.message.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MessageSendRequestDto {
    private String dedupStrategy;
    private long dedupPeriodMinutes;
    private String title;
    private MessageBodyDto body;
    private String recipientType;
    private String recipientId;
}
