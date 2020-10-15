package com.moebius.message;

import com.moebius.message.buffer.MessageSendingBuffer;
import com.moebius.message.domain.BufferedMessages;
import com.moebius.message.domain.DedupStrategy;
import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.domain.MessageSendingResult;
import com.moebius.message.sender.MessageSender;
import com.moebius.message.sender.MessageSenderResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BufferedMessageSendingController {
    private final MessageSendingBuffer messageSendingBuffer;
    private final MessageSenderResolver messageSenderResolver;

    public Flux<MessageSendingResult> sendBufferedMessagesBefore(LocalDateTime deadlineToSendMessage) {
        return messageSendingBuffer.getAllBufferedMessages()
                .filter(bufferedMessage -> bufferedMessage.getFirstReceivedTime().isBefore(deadlineToSendMessage))
                .flatMap(bufferedMessages -> pickMessageSendingRequest(bufferedMessages)
                        .map(messageSendRequest -> sendMessageAndDropFromBuffer(
                                messageSendRequest, bufferedMessages.getMessageKey()
                        ))
                        .orElseGet(() -> dropMessagesFromBufferOnly(bufferedMessages))
                );
    }

    private Optional<MessageSendRequest> pickMessageSendingRequest(BufferedMessages bufferedMessages) {
        DedupStrategy dedupStrategy = bufferedMessages.getDedupStrategy();
        if (DedupStrategy.LEAVE_LAST_ARRIVAL.equals(dedupStrategy)) {
            return Optional.ofNullable(bufferedMessages.getMessageSendRequests())
                    .filter(CollectionUtils::isNotEmpty)
                    .map(requests -> requests.get(requests.size() - 1));
        } else {
            return Optional.empty();
        }
    }

    private Mono<MessageSendingResult> sendMessageAndDropFromBuffer(
            MessageSendRequest messageSendRequest, String messageKey
    ) {
        MessageSender sender = messageSenderResolver.getSender(messageSendRequest.getRecipient().getRecipientType());
        return sender.sendMessage(messageSendRequest)
                .flatMap(result -> messageSendingBuffer.dropKey(messageKey))
                .map(result -> new MessageSendingResult(true, false, true));
    }

    private Mono<MessageSendingResult> dropMessagesFromBufferOnly(BufferedMessages bufferedMessages) {
        return messageSendingBuffer.dropKey(bufferedMessages.getMessageKey())
                .flatMap(dropResult -> Mono.empty());
    }

}
