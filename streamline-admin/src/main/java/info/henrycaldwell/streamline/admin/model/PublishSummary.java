package info.henrycaldwell.streamline.admin.model;

import java.time.Instant;

public record PublishSummary(
    long id,
    long runId,
    String clipId,
    String publisher,
    String uri,
    Instant publishedAt) {
}
