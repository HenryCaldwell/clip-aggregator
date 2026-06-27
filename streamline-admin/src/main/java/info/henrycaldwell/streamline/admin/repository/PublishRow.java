package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record PublishRow(
    long id,
    long runId,
    String clipId,
    String publisher,
    String uri,
    Instant publishedAt) {
}
