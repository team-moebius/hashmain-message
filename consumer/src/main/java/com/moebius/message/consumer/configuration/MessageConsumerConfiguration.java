package com.moebius.message.consumer.configuration;

import com.moebius.message.BufferedMessageSendingController;
import com.moebius.message.consumer.kafka.MessageSendRequestConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@EnableScheduling
public class MessageConsumerConfiguration implements ApplicationListener<ApplicationReadyEvent> {
    private final MessageSendRequestConsumer messageSendRequestConsumer;
    private final BufferedMessageSendingController bufferedMessageSendingController;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        messageSendRequestConsumer.consumeMessages();
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendMessagesOnBuffer(){
        bufferedMessageSendingController.sendBufferedMessagesBefore(LocalDateTime.now());
    }
}
