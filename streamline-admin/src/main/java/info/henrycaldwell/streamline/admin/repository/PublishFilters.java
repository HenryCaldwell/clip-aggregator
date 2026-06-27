package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record PublishFilters(
    String publisher,
    Long runId,
    String clipId,
    Instant from,
    Instant to) {
}
