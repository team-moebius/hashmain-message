package com.moebius.message.sender.slack.template;

import java.util.Map;

public interface ComposeRule {
    String composeValue(Map<String, String> messageParams);
}
