package com.moebius.message.sender.slack.template.rule;

import java.util.Map;

public interface ComposeRule {
    String composeValue(Map<String, String> messageParams);
}
