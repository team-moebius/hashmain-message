package com.moebius.message.sender.slack.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class SlackAttachment {
    private final String color;
    @JsonProperty("author_name")
    private final String author;
    @JsonProperty("author_link")
    private final String authorLink;
    private final String text;
    private final List<Field> fields;
    private final String footer;
    @JsonProperty("ts")
    private final String timestamp;
}
