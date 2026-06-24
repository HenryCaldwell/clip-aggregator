package info.henrycaldwell.streamline.admin.model;

import java.time.Instant;

public record RunSummary(
    long id,
    String runner,
    String status,
    Integer published,
    Instant startedAt,
    Instant endedAt) {
}
