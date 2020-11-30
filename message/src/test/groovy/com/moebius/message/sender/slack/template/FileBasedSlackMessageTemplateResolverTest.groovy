package com.moebius.message.sender.slack.template


import com.moebius.message.sender.slack.template.domain.SlackFieldTemplate
import com.moebius.message.sender.slack.template.rule.OptionalTextRule
import com.moebius.message.sender.slack.template.rule.StaticTextRule
import com.moebius.message.sender.slack.template.rule.TextFormatRule
import com.moebius.message.sender.slack.template.rule.TextRefRule
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

class FileBasedSlackMessageTemplateResolverTest extends Specification {
    def sut = new FileBasedSlackMessageTemplateResolver()
    def webhookUrlInTestTemplate = "https://hooks.slack.com/services/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"

    def "Load template from resource"() {
        given:
        def templateId = "test_template_id"
        when:
        def loadedTemplate = sut.getTemplateById(templateId)
        then:
        assertThat(loadedTemplate, notNullValue())
        assertThat(loadedTemplate.getWebHookUrl(), is(webhookUrlInTestTemplate))
        assertThat(loadedTemplate.attachmentTemplates, hasSize(1))
        def attachmentTemplate = loadedTemplate.attachmentTemplates[0]

        assertThat(attachmentTemplate.color, instanceOf(TextRefRule))
        assertThat(attachmentTemplate.color.composeValue(["color": "#000000"]), is("#000000"))

        assertThat(attachmentTemplate.author, instanceOf(TextRefRule))
        assertThat(attachmentTemplate.author.composeValue(["author": "testAuthor"]), is("testAuthor"))

        assertThat(attachmentTemplate.authorLink, instanceOf(TextRefRule))
        assertThat(attachmentTemplate.authorLink.composeValue(["authorLink": "testAuthorLink"]), is("testAuthorLink"))

        assertThat(attachmentTemplate.text, instanceOf(TextFormatRule))
        assertThat(
                attachmentTemplate.text.composeValue(["tradeSymbol": "BTC", "tradeCreatedAt": "TODAY"]),
                is("[BTC] Heavy trades occurred at TODAY")
        )

        assertThat(attachmentTemplate.fields, hasSize(5))
        def firstFieldTemplate = attachmentTemplate.fields[0]
        assertThat(firstFieldTemplate, instanceOf(SlackFieldTemplate))
        assertThat(firstFieldTemplate.title, instanceOf(StaticTextRule))
        assertThat(firstFieldTemplate.title.composeValue([:]), is("Total ask volume"))
        assertThat(firstFieldTemplate.value, instanceOf(TextFormatRule))
        assertThat(
                firstFieldTemplate.value.composeValue(["tradeVolume":"123", "tradeCurrency":"KRW"]),
                is("123 KRW")
        )

        def secondFieldTemplate = attachmentTemplate.fields[1]
        assertThat(secondFieldTemplate, instanceOf(SlackFieldTemplate))
        assertThat(secondFieldTemplate.title, instanceOf(StaticTextRule))
        assertThat(secondFieldTemplate.title.composeValue([:]), is("Total bid volume"))
        assertThat(secondFieldTemplate.value, instanceOf(TextFormatRule))
        assertThat(
                secondFieldTemplate.value.composeValue(["bidVolume":"123", "bidCurrency":"KRW"]),
                is("123 KRW")
        )

        def thirdFieldTemplate = attachmentTemplate.fields[2]
        assertThat(thirdFieldTemplate, instanceOf(SlackFieldTemplate))
        assertThat(thirdFieldTemplate.title, instanceOf(StaticTextRule))
        assertThat(thirdFieldTemplate.title.composeValue([:]), is("Current Price"))
        assertThat(thirdFieldTemplate.value, instanceOf(TextFormatRule))
        assertThat(
                thirdFieldTemplate.value.composeValue(["tradePrice":"123", "unitCurrency":"KRW"]),
                is("123 KRW")
        )

        def fourthFieldTemplate = attachmentTemplate.fields[3]
        assertThat(fourthFieldTemplate, instanceOf(SlackFieldTemplate))
        assertThat(fourthFieldTemplate.title, instanceOf(StaticTextRule))
        assertThat(fourthFieldTemplate.title.composeValue([:]), is("Price change"))
        assertThat(fourthFieldTemplate.value, instanceOf(TextFormatRule))
        assertThat(
                fourthFieldTemplate.value.composeValue(["priceChange":"123", "unitCurrency":"KRW", "priceChangeRatio": "34"]),
                is("123 KRW (34%)")
        )

        def fifthFieldTemplate = attachmentTemplate.fields[4]
        assertThat(fifthFieldTemplate, instanceOf(SlackFieldTemplate))
        assertThat(fifthFieldTemplate.title, instanceOf(OptionalTextRule))
        assertThat(fifthFieldTemplate.title.composeValue([:]), nullValue())
        assertThat(fifthFieldTemplate.title.composeValue(["subscribers": ""]), nullValue())
        assertThat(fifthFieldTemplate.title.composeValue(["subscribers":"<@U019YV88QBV>"]), is("Subscribers"))
        assertThat(fifthFieldTemplate.value, instanceOf(TextRefRule))
        assertThat(
                fifthFieldTemplate.value.composeValue(["subscribers":"<@U019YV88QBV>"]),
                is("<@U019YV88QBV>")
        )

        assertThat(attachmentTemplate.footer, nullValue())
    }

}
