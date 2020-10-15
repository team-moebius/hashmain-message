package com.moebius.message.sender.slack.template;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SlackFieldTemplate {
    private final ComposeRule title;
    private final ComposeRule value;
}
