package com.moebius.message.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class MessageSendRequest {
    private final DedupParameters dedupParameters;
    private final String title;
    private final MessageBody body;
    private final Recipient recipient;
}
