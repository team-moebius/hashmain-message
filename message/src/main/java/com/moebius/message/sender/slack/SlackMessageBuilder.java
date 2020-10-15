package com.moebius.message.sender.slack;

import com.moebius.message.domain.MessageBody;
import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.sender.slack.dto.Field;
import com.moebius.message.sender.slack.dto.SlackAttachment;
import com.moebius.message.sender.slack.dto.SlackMessageDto;
import com.moebius.message.sender.slack.template.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SlackMessageBuilder {
    private final SlackMessageTemplateResolver templateResolver;

    public SlackMessageDto buildMessage(MessageSendRequest request){
        String templateId = getTemplateId(request);

        SlackMessageTemplate messageTemplate = templateResolver.getTemplateById(templateId);

        List<SlackAttachment> attachments = messageTemplate.getAttachmentTemplates().stream()
                .map(attachmentTemplate -> buildAttachment(attachmentTemplate, request))
                .collect(Collectors.toList());

        return SlackMessageDto.builder()
                .attachments(attachments)
                .build();
    }

    private String getTemplateId(MessageSendRequest request) {
        return Optional.ofNullable(request.getBody())
                .map(MessageBody::getTemplateId)
                .orElseThrow();
    }

    private SlackAttachment buildAttachment(SlackAttachmentTemplate attachmentTemplate, MessageSendRequest request){
        Map<String, String> messageParameters = request.getBody().getParameters();

        String color = buildFieldOrNull(attachmentTemplate.getColor(), messageParameters);
        String author = buildFieldOrNull(attachmentTemplate.getAuthor(), messageParameters);
        String authorLink = buildFieldOrNull(attachmentTemplate.getAuthorLink(), messageParameters);
        String text = buildFieldOrNull(attachmentTemplate.getText(), messageParameters);
        List<Field> fields = buildFields(attachmentTemplate, messageParameters);
        String footer = buildFieldOrNull(attachmentTemplate.getFooter(), messageParameters);

        return SlackAttachment.builder()
                .color(color)
                .author(author)
                .authorLink(authorLink)
                .text(text)
                .fields(fields)
                .footer(footer)
                .build();
    }

    private List<Field> buildFields(SlackAttachmentTemplate attachmentTemplate, Map<String, String> messageParameters) {
        return attachmentTemplate.getFields().stream()
                .map(fieldTemplate -> this.buildField(fieldTemplate, messageParameters))
                .collect(Collectors.toList());
    }

    private Field buildField(SlackFieldTemplate fieldTemplate, Map<String, String> messageParameters) {
        String title = buildFieldOrNull(fieldTemplate.getTitle(), messageParameters);
        String value = buildFieldOrNull(fieldTemplate.getValue(), messageParameters);

        return Field.builder()
                .title(title)
                .value(value)
                .build();
    }

    private String buildFieldOrNull(ComposeRule composeRule, Map<String, String> messageParameters) {
        return Optional.ofNullable(composeRule)
                .map(rule->rule.composeValue(messageParameters))
                .orElse(null);
    }
}
