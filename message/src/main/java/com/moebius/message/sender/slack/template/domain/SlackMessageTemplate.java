package com.moebius.message.sender.slack.template.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@RequiredArgsConstructor
public class SlackMessageTemplate {
    private final String webHookUrl;
    private final List<SlackAttachmentTemplate> attachmentTemplates;

    public SlackMessageTemplate() {
        this.webHookUrl = null;
        this.attachmentTemplates = new ArrayList<>();
    }
}
