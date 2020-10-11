package com.moebius.message.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageSendingResult {
    private final boolean sent;
    private final boolean msgSavedToBuffer;
    private final boolean result;
}
