package info.henrycaldwell.streamline.admin.model;

import java.time.Instant;

public record AttemptSummary(
    long id,
    long runId,
    String worker,
    String clipId,
    String stage,
    String component,
    String status,
    String error,
    Instant startedAt,
    Instant endedAt) {
}
