package com.moebius.message.sender.slack.template.rule;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class StaticTextRule implements ComposeRule {
    private final String staticText;

    @Override
    public String composeValue(Map<String, String> messageParams) {
        return staticText;
    }
}
