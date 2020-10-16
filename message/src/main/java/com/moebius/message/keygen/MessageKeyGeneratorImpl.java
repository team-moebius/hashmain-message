package com.moebius.message.keygen;

import com.moebius.message.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class MessageKeyGeneratorImpl implements MessageKeyGenerator {
    private final static String DELIMITER_FOR_MESSAGE_KEY = "-";
    @Override
    public String generateMessageKey(MessageSendRequest messageSendRequest) {
        validateRequest(messageSendRequest);
        DedupStrategy dedupStrategy = messageSendRequest.getDedupParameters().getDedupStrategy();
        RecipientType recipientType = messageSendRequest.getRecipient().getRecipientType();
        String title = messageSendRequest.getTitle();

        return StringUtils.joinWith(DELIMITER_FOR_MESSAGE_KEY, dedupStrategy, recipientType, title);
    }

    private void validateRequest(MessageSendRequest messageSendRequest) {
        if (Objects.isNull(messageSendRequest)){
            throw new IllegalArgumentException("Request must not be null");
        }

        if (Objects.isNull(messageSendRequest.getDedupParameters().getDedupStrategy())){
            throw new IllegalArgumentException("DedupStrategy must not be null");
        }

        Optional<RecipientType> optionalRecipientType = Optional.ofNullable(messageSendRequest.getRecipient())
                .map(Recipient::getRecipientType);

        if (optionalRecipientType.isEmpty()){
            throw new IllegalArgumentException("Recipient or RecipientType must not be null");
        }

        if (StringUtils.isEmpty(messageSendRequest.getTitle())){
            throw new IllegalArgumentException("Title must not be null");
        }
    }
}
