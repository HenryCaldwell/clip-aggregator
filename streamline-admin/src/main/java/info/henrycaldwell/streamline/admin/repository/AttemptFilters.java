package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record AttemptFilters(
    String status,
    Long runId,
    String clipId,
    String stage,
    Instant from,
    Instant to) {
}
