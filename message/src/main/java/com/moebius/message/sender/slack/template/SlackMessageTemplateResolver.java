package com.moebius.message.sender.slack.template;

public interface SlackMessageTemplateResolver {
    SlackMessageTemplate getTemplateById(String templateId);
}
