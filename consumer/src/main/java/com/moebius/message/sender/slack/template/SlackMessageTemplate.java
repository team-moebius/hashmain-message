package com.moebius.message.sender.slack.template;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class SlackMessageTemplate {
    private final List<SlackAttachmentTemplate> attachmentTemplates;
}
