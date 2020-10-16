package com.moebius.message.sender.slack.template.domain;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@RequiredArgsConstructor
public class SlackMessageTemplate {
    private final List<SlackAttachmentTemplate> attachmentTemplates;

    public SlackMessageTemplate() {
        this.attachmentTemplates = new ArrayList<>();
    }
}
