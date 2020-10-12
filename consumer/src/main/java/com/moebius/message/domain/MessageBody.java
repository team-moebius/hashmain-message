package com.moebius.message.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;


@Builder
@AllArgsConstructor
@Getter
public class MessageBody {
    private final String templateId;
    private final Map<String, String> parameters;
}
