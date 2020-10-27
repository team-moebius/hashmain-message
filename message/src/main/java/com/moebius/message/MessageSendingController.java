package com.moebius.message;

import com.moebius.message.buffer.MessageSendingBuffer;
import com.moebius.message.domain.DedupParameters;
import com.moebius.message.domain.DedupStrategy;
import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.domain.MessageSendingResult;
import com.moebius.message.keygen.MessageKeyGenerator;
import com.moebius.message.sender.MessageSender;
import com.moebius.message.sender.MessageSenderResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MessageSendingController {
    private final MessageKeyGenerator messageKeyGenerator;
    private final MessageSendingBuffer messageSendingBuffer;
    private final MessageSenderResolver messageSenderResolver;

    public Mono<MessageSendingResult> receiveMessageSendRequest(MessageSendRequest messageSendRequest) {
        String messageKey = messageKeyGenerator.generateMessageKey(messageSendRequest);
        return Mono.just(getDedupStrategyFromRequest(messageSendRequest))
                .flatMap(dedupStrategy -> Mono.zip(
                        shouldSendMessage(messageKey, dedupStrategy), shouldPutBuffer(dedupStrategy)
                        )
                )
                .flatMap(conditionPair -> saveToBuffer(messageSendRequest, messageKey, conditionPair.getT2())
                        .map(result -> conditionPair)
                )
                .filter(Tuple2::getT1)
                .flatMap(conditionPair -> sendMessage(messageSendRequest, conditionPair.getT2()))
                .switchIfEmpty(createResultOnly());
    }

    private DedupStrategy getDedupStrategyFromRequest(MessageSendRequest request) {
        return Optional.ofNullable(request.getDedupParameters())
                .map(DedupParameters::getDedupStrategy)
                .orElse(DedupStrategy.NO_DEDUP);
    }

    private Mono<Boolean> shouldSendMessage(String messageKey, DedupStrategy dedupStrategy) {
        if (DedupStrategy.NO_DEDUP.equals(dedupStrategy)) {
            return Mono.just(true);
        } else if (DedupStrategy.LEAVE_FIRST_ARRIVAL.equals(dedupStrategy)) {
            return messageSendingBuffer.hasDuplicatedMessageWith(messageKey)
                    .map(hasDuplicatedMessage -> !hasDuplicatedMessage);
        } else {
            return Mono.just(false);
        }
    }

    private Mono<Boolean> shouldPutBuffer(DedupStrategy dedupStrategy) {
        return Mono.just(!DedupStrategy.NO_DEDUP.equals(dedupStrategy));
    }

    private Mono<Boolean> saveToBuffer(MessageSendRequest messageSendRequest, String messageKey, Boolean shouldSaveBuffer) {
        if (shouldSaveBuffer) {
            return messageSendingBuffer.put(messageKey, messageSendRequest);
        }
        return Mono.just(false);
    }

    private Mono<MessageSendingResult> sendMessage(MessageSendRequest messageSendRequest, boolean savedToBuffer) {
        MessageSender messageSender = messageSenderResolver.getSender(
                messageSendRequest.getRecipient().getRecipientType()
        );

        return messageSender.sendMessage(messageSendRequest)
                .map(sentResult -> new MessageSendingResult(true, savedToBuffer, sentResult));
    }

    private Mono<MessageSendingResult> createResultOnly() {
        return Mono.just(new MessageSendingResult(false, true, true));
    }
}
