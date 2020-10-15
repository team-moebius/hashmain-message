package com.moebius.message.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class BufferedMessages {
    private final DedupStrategy dedupStrategy;
    private final LocalDateTime firstReceivedTime;
    private final List<MessageSendRequest> messageSendRequests;
    private final String messageKey;
}
