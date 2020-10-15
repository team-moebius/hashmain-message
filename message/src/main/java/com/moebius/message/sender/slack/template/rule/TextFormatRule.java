package com.moebius.message.sender.slack.template.rule;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class TextFormatRule implements ComposeRule {
    private final static String PLACE_HOLDER_FORMAT = "${%s}";
    private final String formatString;
    private final List<String> targetFields;

    @Override
    public String composeValue(Map<String, String> messageParams) {
        return targetFields.stream()
                .reduce(formatString, (replacedString, fieldName) -> replacedString.replace(
                        getPlaceHolder(fieldName), messageParams.getOrDefault(fieldName, "")
                ));
    }

    private String getPlaceHolder(String key) {
        return String.format(PLACE_HOLDER_FORMAT, key);
    }
}
