package com.moebius.message.sender.slack

import com.moebius.message.domain.*
import com.moebius.message.sender.slack.template.*
import com.moebius.message.sender.slack.template.domain.SlackAttachmentTemplate
import com.moebius.message.sender.slack.template.domain.SlackFieldTemplate
import com.moebius.message.sender.slack.template.domain.SlackMessageTemplate
import com.moebius.message.sender.slack.template.rule.StaticTextRule
import com.moebius.message.sender.slack.template.rule.TextFormatRule
import com.moebius.message.sender.slack.template.rule.TextRefRule
import spock.lang.Specification

class SlackMessageBuilderTest extends Specification {
    def mockTemplateResolver = Mock(SlackMessageTemplateResolver)

    def sut = new SlackMessageBuilder(mockTemplateResolver)

    def "Test Build message with valid template"() {
        given:
        def messageTemplate = new SlackMessageTemplate([
                new SlackAttachmentTemplate(
                        new TextRefRule("color"), new TextRefRule("author"), new TextRefRule("authorLink"),
                        new TextFormatRule("[\${tradeSymbol}] Heavy trades occurred at \${tradeCreatedAt}", ["tradeSymbol", "tradeCreatedAt"]),
                        [
                                new SlackFieldTemplate(new StaticTextRule("Total ask volume"), new TextFormatRule("\${tradeVolume} \${tradeCurrency}", ["tradeVolume", "tradeCurrency"])),
                                new SlackFieldTemplate(new StaticTextRule("Total bid volume"), new TextFormatRule("\${bidVolume} \${bidCurrency}", ["bidVolume", "bidCurrency"])),
                                new SlackFieldTemplate(new StaticTextRule("Current Price"), new TextFormatRule("\${tradePrice} \${unitCurrency}", ["tradePrice", "unitCurrency"])),
                                new SlackFieldTemplate(new StaticTextRule("Price change"), new TextFormatRule("\${priceChange} \${unitCurrency} (\${priceChangeRatio}%)", ["priceChange", "unitCurrency", "priceChangeRatio"])),
                        ], null
                )]
        )

        def templateId = "TEST_TEMPLATE_ID"

        def messageParam = [
                "color": "#0051C7", "author": "test-sender", "authorLink": "http://test-message.com",
                "tradeSymbol": "BTC", "tradeCreatedAt": "17:30:24", "tradeVolume": "123", "tradeCurrency": "BTC",
                "bidVolume": "456", "bidCurrency": "BTC", "tradePrice": "13,927,348",
                "priceChange": "-123,123", "unitCurrency": "KRW", "priceChangeRatio": "-23",
        ]

        def request = new MessageSendRequest(
                DedupStrategy.NO_DEDUP, "Some test title",
                new MessageBody(templateId, messageParam),
                new Recipient(RecipientType.SLACK, "#test_channel")
        )

        1 * mockTemplateResolver.getTemplateById(templateId) >> messageTemplate

        when:
        def message = sut.buildMessage(request)

        then:
        message.attachments.size() == 1
        def attachment = message.attachments[0]

        attachment.color == messageParam["color"]
        attachment.author == messageParam["author"]
        attachment.authorLink == messageParam["authorLink"]
        attachment.text == "[${messageParam['tradeSymbol']}] Heavy trades occurred at ${messageParam['tradeCreatedAt']}"

        attachment.fields[0].title == "Total ask volume"
        attachment.fields[0].value == "${messageParam['tradeVolume']} ${messageParam['tradeCurrency']}"

        attachment.fields[1].title == "Total bid volume"
        attachment.fields[1].value == "${messageParam['bidVolume']} ${messageParam['bidCurrency']}"

        attachment.fields[2].title == "Current Price"
        attachment.fields[2].value == "${messageParam['tradePrice']} ${messageParam['unitCurrency']}"

        attachment.fields[3].title == "Price change"
        attachment.fields[3].value == "${messageParam['priceChange']} ${messageParam['unitCurrency']} (${messageParam['priceChangeRatio']}%)"

    }

}
