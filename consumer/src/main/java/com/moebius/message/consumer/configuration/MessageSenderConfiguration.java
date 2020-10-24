package com.moebius.message.consumer.configuration;

import com.moebius.message.MessageSendController;
import com.moebius.message.sender.MessageSender;
import com.moebius.message.sender.MessageSenderResolver;
import com.moebius.message.sender.slack.SlackMessageSender;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@ComponentScan(basePackageClasses = MessageSendController.class)
public class MessageSenderConfiguration {
    private final SlackMessageSender slackMessageSender;

    @Bean
    public MessageSenderResolver messageSenderResolver(){
        List<MessageSender> senders = new ArrayList<>();
        senders.add(slackMessageSender);
        return new MessageSenderResolver(senders);
    }
}
