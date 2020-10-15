package com.moebius.message.sender.slack.template;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class SlackAttachmentTemplate {
    private final ComposeRule color;
    private final ComposeRule author;
    private final ComposeRule authorLink;
    private final ComposeRule text;
    private final List<SlackFieldTemplate> fields;
    private final ComposeRule footer;
}
