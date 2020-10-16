package com.moebius.message.keygen

import com.moebius.message.domain.DedupParameters
import com.moebius.message.domain.DedupStrategy
import com.moebius.message.domain.MessageSendRequest
import com.moebius.message.domain.Recipient
import com.moebius.message.domain.RecipientType
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.StringContains.containsString

class MessageKeyGeneratorImplTest extends Specification {
    def sut = new MessageKeyGeneratorImpl()

    def "Test generate messageKey from valid request"() {
        given:
        def request = MessageSendRequest.builder()
                .dedupParameters(DedupParameters.builder()
                        .dedupStrategy(dedupStrategy)
                        .dedupPeriodMinutes(1)
                        .build())
                .title(title)
                .recipient(Recipient
                        .builder()
                        .recipientId("")
                        .recipientType(recipientType)
                        .build()
                )
                .build()

        when:
        def messageKey = sut.generateMessageKey(request)

        then:
        assertThat(messageKey, containsString(dedupStrategy.name()))
        assertThat(messageKey, containsString(title))
        assertThat(messageKey, containsString(recipientType.name()))

        where:
        dedupStrategy                     | title        | recipientType
        DedupStrategy.LEAVE_LAST_ARRIVAL  | "testTitle1" | RecipientType.EMAIL
        DedupStrategy.LEAVE_LAST_ARRIVAL  | "testTitle2" | RecipientType.SLACK
        DedupStrategy.LEAVE_FIRST_ARRIVAL | "testTitle3" | RecipientType.EMAIL
        DedupStrategy.LEAVE_FIRST_ARRIVAL | "testTitle4" | RecipientType.SLACK
        DedupStrategy.NO_DEDUP            | "testTitle5" | RecipientType.EMAIL
        DedupStrategy.NO_DEDUP            | "testTitle6" | RecipientType.SLACK
    }

    def "Test generate messageKey from invalid request"() {
        given:
        def request = MessageSendRequest.builder()
                .dedupParameters(DedupParameters.builder()
                        .dedupStrategy(dedupStrategy)
                        .dedupPeriodMinutes(5)
                        .build())
                .title(title)
                .recipient(Recipient
                        .builder()
                        .recipientId("")
                        .recipientType(recipientType)
                        .build()
                )
                .build()

        when:
        sut.generateMessageKey(request)

        then:
        thrown(IllegalArgumentException.class)

        where:
        dedupStrategy                     | title        | recipientType
        null                              | "testTitle1" | RecipientType.EMAIL
        DedupStrategy.LEAVE_LAST_ARRIVAL  | "testTitle1" | null
        DedupStrategy.LEAVE_FIRST_ARRIVAL | ""           | RecipientType.EMAIL
        DedupStrategy.LEAVE_FIRST_ARRIVAL | null         | RecipientType.EMAIL
    }
}
