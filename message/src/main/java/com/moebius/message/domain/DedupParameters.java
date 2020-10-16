package com.moebius.message.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
public class DedupParameters {
    private final DedupStrategy dedupStrategy;
    private final long dedupPeriodMinutes;

    public static DedupParameters noDedup(){
        return new DedupParameters(DedupStrategy.NO_DEDUP, 0);
    }

}
