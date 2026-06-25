package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record FetchFilters(
    String status,
    String retriever,
    Long runId,
    Instant from,
    Instant to) {
}
