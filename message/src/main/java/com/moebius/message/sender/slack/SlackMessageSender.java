package com.moebius.message.sender.slack;

import com.moebius.message.domain.MessageSendRequest;
import com.moebius.message.sender.MessageSender;
import groovy.util.logging.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class SlackMessageSender implements MessageSender {
    private final WebClient webClient;
    private final SlackMessageBuilder slackMessageBuilder;
    private final String webHookUrl;

    public SlackMessageSender(WebClient webClient, SlackMessageBuilder slackMessageBuilder, String webHookUrl) {
        this.webClient = webClient;
        this.slackMessageBuilder = slackMessageBuilder;
        this.webHookUrl = webHookUrl;
    }

    @Override
    public Mono<Boolean> sendMessage(MessageSendRequest messageSendRequest) {
        return Mono.just(slackMessageBuilder.buildMessage(messageSendRequest))
                .flatMap(slackMessageDto -> webClient.post()
                            .uri(webHookUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(slackMessageDto)
                            .exchange()
                )
                .map(clientResponse -> {
                    if (clientResponse.statusCode().isError()){
                        throw new RuntimeException("Slack Message send failed due to server error");
                    }
                    return true;
                });
    }
}