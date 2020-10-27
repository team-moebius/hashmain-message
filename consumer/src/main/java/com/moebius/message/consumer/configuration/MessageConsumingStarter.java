package com.moebius.message.consumer.configuration;

import com.moebius.message.consumer.kafka.MessageSendRequestConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageConsumingStarter implements ApplicationListener<ApplicationReadyEvent> {
    private final MessageSendRequestConsumer messageSendRequestConsumer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        messageSendRequestConsumer.consumeMessages();
    }

}
