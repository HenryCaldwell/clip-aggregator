package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record AttemptRow(
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
