package com.moebius.message.sender;

import com.moebius.message.domain.RecipientType;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageSenderResolver {
    private final Map<RecipientType, MessageSender> messageSenderByType;

    public MessageSenderResolver(List<MessageSender> senders) {
        messageSenderByType = senders.stream()
                .collect(Collectors.toMap(MessageSender::getRecipientType, messageSender -> messageSender));
    }

    public MessageSender getSender(RecipientType recipientType){
        return messageSenderByType.computeIfAbsent(recipientType, missingType -> {
            throw new NotImplementedException(String.format(
                    "Sender for %s type is not implemented", recipientType.name()
            ));
        });
    }
}
