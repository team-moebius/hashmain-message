package com.moebius.message.sender.slack

import com.moebius.message.domain.*
import com.moebius.message.sender.slack.dto.SlackMessageDto
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import spock.lang.Specification

class SlackMessageSenderTest extends Specification {
    def mockWebClient = Mock(WebClient)
    def messageBuilder = Mock(SlackMessageBuilder)
    def webHookUrl = "https://hooks.slack.com/services/SOME/WEBHOOK/URL"

    def sut = new SlackMessageSender(mockWebClient, messageBuilder)

    def mockSlackMessageDto = Mock(SlackMessageDto)
    def mockRequestBodyBuilder = Mock(WebClient.RequestBodyUriSpec)
    def mockResponse = Mock(ClientResponse)

    def setup(){
        mockSlackMessageDto.getWebHookUrl() >> webHookUrl
    }

    def "Test message sending"() {
        given:
        def request = new MessageSendRequest(
                DedupParameters.noDedup(), title, new MessageBody(templateId, params),
                Recipient.builder()
                        .recipientId("some channel name")
                        .recipientType(RecipientType.SLACK)
                        .build()
        )
        1 * messageBuilder.buildMessage(request) >> mockSlackMessageDto
        1 * mockWebClient.post() >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.uri(webHookUrl) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.contentType(MediaType.APPLICATION_JSON) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.bodyValue(mockSlackMessageDto) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.exchange() >> Mono.just(mockResponse)
        1 * mockResponse.statusCode() >> HttpStatus.OK

        expect:
        StepVerifier.create(sut.sendMessage(request))
            .assertNext({it})
            .verifyComplete()
        where:
        title           |   templateId      |   params
        "testTitle1"    |   "TEST_TMPL_1"   |   ["a": "1", "b": "2", "c": "3"]
        "testTitle2"    |   "TEST_TMPL_2"   |   Collections.<String, String>emptyMap()
    }

    def "Test message send failure"() {
        def request = new MessageSendRequest(
                DedupParameters.noDedup(), title, new MessageBody(templateId, params),
                Recipient.builder()
                        .recipientId("some channel name")
                        .recipientType(RecipientType.SLACK)
                        .build()
        )
        1 * messageBuilder.buildMessage(request) >> mockSlackMessageDto
        1 * mockWebClient.post() >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.uri(webHookUrl) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.contentType(MediaType.APPLICATION_JSON) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.bodyValue(mockSlackMessageDto) >> mockRequestBodyBuilder
        1 * mockRequestBodyBuilder.exchange() >> Mono.just(mockResponse)
        1 * mockResponse.statusCode() >> HttpStatus.INTERNAL_SERVER_ERROR

        expect:
        StepVerifier.create(sut.sendMessage(request))
                .expectError()
                .verify()
        where:
        title           |   templateId      |   params
        "testTitle1"    |   "TEST_TMPL_1"   |   ["a": "1", "b": "2", "c": "3"]
    }
}
