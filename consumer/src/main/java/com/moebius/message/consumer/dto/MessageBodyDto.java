package com.moebius.message.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class MessageBodyDto {
    private final String templateId;
    private final Map<String, String> parameters;
}
