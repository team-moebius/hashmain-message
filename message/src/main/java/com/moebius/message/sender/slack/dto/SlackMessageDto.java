package com.moebius.message.sender.slack.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class SlackMessageDto {
    private final List<SlackAttachment> attachments;
}
