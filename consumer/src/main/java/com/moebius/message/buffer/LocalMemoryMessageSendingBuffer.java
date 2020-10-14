package com.moebius.message.buffer;

import com.moebius.message.domain.BufferedMessages;
import com.moebius.message.domain.MessageSendRequest;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

public class LocalMemoryMessageSendingBuffer implements MessageSendingBuffer {
    private static final Map<String, BufferedMessages> bufferedMessages = new Hashtable<>();
    @Override
    public Mono<Boolean> hasDuplicatedMessageWith(String messageKey) {
        return Mono.just(bufferedMessages.containsKey(messageKey));
    }

    @Override
    public Mono<Boolean> put(String messageKey, MessageSendRequest messageSendRequest) {
        return validateMessage(messageKey, messageSendRequest)
                .switchIfEmpty(insertMessageSendRequest(messageKey, messageSendRequest));
    }

    private Mono<Boolean> validateMessage(String messageKey, MessageSendRequest messageSendRequest) {
        if (StringUtils.isEmpty(messageKey)){
            return Mono.error(new IllegalArgumentException("Message Key must not be empty"));
        }

        if (Objects.isNull(messageSendRequest)){
            Mono.error(new IllegalArgumentException("MessageSendRequest must not be null"));
        }

        if (Objects.isNull(messageSendRequest.getDedupStrategy())){
            Mono.error(new IllegalArgumentException("DedupStrategy of MessageSendRequest must not be null"));
        }
        return Mono.empty();
    }

    @Override
    public Flux<BufferedMessages> getAllBufferedMessages() {
        return Flux.fromIterable(bufferedMessages.values());
    }

    @Override
    public Mono<Boolean> dropKey(String messageKey) {
        if (bufferedMessages.containsKey(messageKey)){
            return Mono.just(bufferedMessages.remove(messageKey) != null);
        } else {
            return Mono.just(false);
        }
    }

    private Mono<Boolean> insertMessageSendRequest(String messageKey, MessageSendRequest messageSendRequest){
        BufferedMessages bufferedMessages = LocalMemoryMessageSendingBuffer.bufferedMessages.computeIfAbsent(
                messageKey, missingKey -> createBufferedMessages(messageKey, messageSendRequest)
        );
        bufferedMessages.getMessageSendRequests().add(messageSendRequest);
        return Mono.just(true);
    }

    private BufferedMessages createBufferedMessages(String messageKey, MessageSendRequest messageSendRequest){
        return new BufferedMessages(
                messageSendRequest.getDedupStrategy(), LocalDateTime.now(), new LinkedList<>(), messageKey
        );
    }
}
