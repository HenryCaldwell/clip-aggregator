package info.henrycaldwell.streamline.admin.model;

import java.time.Instant;

public record FetchSummary(
    long id,
    long runId,
    String retriever,
    String status,
    String error,
    Integer clips,
    Instant startedAt,
    Instant endedAt) {
}
