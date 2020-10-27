package com.moebius.message.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MessageBodyDto {
    private String templateId;
    private Map<String, String> parameters;
}
