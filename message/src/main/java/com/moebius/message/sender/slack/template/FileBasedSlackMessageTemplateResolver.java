package com.moebius.message.sender.slack.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.moebius.message.sender.slack.template.deserializer.ComposeRuleDeserializer;
import com.moebius.message.sender.slack.template.domain.SlackMessageTemplate;
import com.moebius.message.sender.slack.template.rule.ComposeRule;

import java.io.IOException;


public class FileBasedSlackMessageTemplateResolver implements SlackMessageTemplateResolver {
    private final static String BASE_TEMPLATE_RESOURCE_PATH = "/templates/slack/%s.json";
    private final ObjectMapper objectMapper;

    public FileBasedSlackMessageTemplateResolver() {
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ComposeRule.class, new ComposeRuleDeserializer());
        objectMapper.registerModule(module);

    }

    @Override
    public SlackMessageTemplate getTemplateById(String templateId) {
        try {
            return objectMapper.readValue(
                    getClass().getResource(String.format(BASE_TEMPLATE_RESOURCE_PATH, templateId)),
                    SlackMessageTemplate.class
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
