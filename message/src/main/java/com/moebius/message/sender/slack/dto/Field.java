package com.moebius.message.sender.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Field {
    private final String title;
    private final String value;
    @JsonProperty("short")
    private final boolean isShort;
}
