package com.moebius.message.sender.slack.template.rule;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class OptionalTextRule implements ComposeRule {
    private final String text;
    private final String targetField;

    public OptionalTextRule(String text, String targetField) {
        this.text = text;
        this.targetField = targetField;
    }

    @Override
    public String composeValue(Map<String, String> messageParams) {
        if (StringUtils.isNoneEmpty(messageParams.getOrDefault(targetField, null))) {
            return text;
        }
        return null;
    }
}
