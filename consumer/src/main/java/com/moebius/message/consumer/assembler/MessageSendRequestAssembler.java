package com.moebius.message.consumer.assembler;

import com.moebius.backend.dto.message.MessageSendRequestDto;
import com.moebius.message.domain.*;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MessageSendRequestAssembler {
    public MessageSendRequest assembleMessageSendRequest(MessageSendRequestDto requestDto){
        return MessageSendRequest.builder()
                .dedupParameters(assembleDedupParameter(requestDto))
                .title(requestDto.getTitle())
                .body(assembleBody(requestDto))
                .recipient(assembleRecipient(requestDto))
                .build();
    }

    private Recipient assembleRecipient(MessageSendRequestDto requestDto) {
        RecipientType recipientType = EnumUtils.getEnum(RecipientType.class, requestDto.getRecipientType());
        String recipientId = requestDto.getRecipientId();

        return Recipient.builder()
                .recipientType(recipientType)
                .recipientId(recipientId)
                .build();
    }

    private MessageBody assembleBody(MessageSendRequestDto requestDto) {
        return Optional.ofNullable(requestDto.getBody())
                .map(bodyDto ->MessageBody.builder()
                        .templateId(bodyDto.getTemplateId())
                        .parameters(bodyDto.getParameters())
                        .build())
                .orElseThrow();
    }

    private DedupParameters assembleDedupParameter(MessageSendRequestDto requestDto) {
        DedupStrategy dedupStrategy = EnumUtils.getEnum(
                DedupStrategy.class, requestDto.getDedupStrategy(), DedupStrategy.NO_DEDUP
        );

        return DedupParameters.builder()
                .dedupStrategy(dedupStrategy)
                .dedupPeriodMinutes(requestDto.getDedupPeriodMinutes())
                .build();
    }
}
