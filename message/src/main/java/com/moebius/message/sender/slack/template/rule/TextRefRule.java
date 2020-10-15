package com.moebius.message.sender.slack.template.rule;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class TextRefRule implements ComposeRule {
    private final String targetField;

    @Override
    public String composeValue(Map<String, String> messageParams) {
        return messageParams.getOrDefault(targetField, "");
    }
}
