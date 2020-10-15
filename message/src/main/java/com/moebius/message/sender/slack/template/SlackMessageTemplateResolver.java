package com.moebius.message.sender.slack.template;

import com.moebius.message.sender.slack.template.domain.SlackMessageTemplate;

public interface SlackMessageTemplateResolver {
    SlackMessageTemplate getTemplateById(String templateId);
}
