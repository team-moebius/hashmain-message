package com.moebius.message.sender.slack.template.domain;

import com.moebius.message.sender.slack.template.rule.ComposeRule;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Builder
@Getter
@RequiredArgsConstructor
public class SlackFieldTemplate {
    private final ComposeRule title;
    private final ComposeRule value;

    public SlackFieldTemplate() {
        title = null;
        value = null;
    }
}
