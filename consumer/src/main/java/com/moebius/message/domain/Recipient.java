package com.moebius.message.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Recipient {
    private final RecipientType recipientType;
    private final String recipientId;
}
