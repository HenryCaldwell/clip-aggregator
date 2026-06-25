package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record FetchRow(
    long id,
    long runId,
    String retriever,
    String status,
    String error,
    Integer clips,
    Instant startedAt,
    Instant endedAt) {
}
