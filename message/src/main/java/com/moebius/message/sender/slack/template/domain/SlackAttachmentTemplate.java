package com.moebius.message.sender.slack.template.domain;

import com.moebius.message.sender.slack.template.rule.ComposeRule;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
@RequiredArgsConstructor
public class SlackAttachmentTemplate {
    private final ComposeRule color;
    private final ComposeRule author;
    private final ComposeRule authorLink;
    private final ComposeRule text;
    private final List<SlackFieldTemplate> fields;
    private final ComposeRule footer;

    public SlackAttachmentTemplate() {
        color = null;
        author = null;
        authorLink = null;
        text = null;
        fields = Collections.emptyList();
        footer = null;
    }
}
