package com.moebius.message.consumer.configuration;

import com.moebius.message.BufferedMessageSendingController;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class CronBufferedMessageSendingTrigger {
    private final BufferedMessageSendingController bufferedMessageSendingController;

    @Scheduled(cron = "0 * * * * *")
    public void sendMessagesOnBuffer() {
        bufferedMessageSendingController.sendBufferedMessagesBefore(LocalDateTime.now())
                .subscribe();
    }
}
