package com.ruoyi.intervention.domain.model;

import java.time.LocalDateTime;

/**
 * A single time-stamped observation for a health metric.
 *
 * Reference: Klasnja et al. 2015 JITAI; Nahum-Shani et al. 2018 JITAI design
 */
public record TimeSeriesPoint(
    String metric,
    double value,
    LocalDateTime timestamp,
    String source,
    String evidenceRef
) {
    public TimeSeriesPoint(String metric, double value, LocalDateTime timestamp) {
        this(metric, value, timestamp, null, "klasnja_2015_jitai");
    }

    public TimeSeriesPoint(String metric, double value, LocalDateTime timestamp, String source) {
        this(metric, value, timestamp, source, "klasnja_2015_jitai");
    }
}
