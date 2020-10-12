package com.moebius.message.domain;

import com.moebius.message.dedup.DedupStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class MessageSendRequest {
    private final DedupStrategy dedupStrategy;
    private final String title;
    private final MessageBody body;
    private final Recipient recipient;
}
