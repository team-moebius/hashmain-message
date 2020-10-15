package com.moebius.message.sender.slack.template.domain;

import com.moebius.message.sender.slack.template.rule.ComposeRule;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SlackFieldTemplate {
    private final ComposeRule title;
    private final ComposeRule value;
}
