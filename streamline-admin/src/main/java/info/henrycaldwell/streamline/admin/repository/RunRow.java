package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record RunRow(
    long id,
    String runner,
    String status,
    Integer published,
    Instant startedAt,
    Instant endedAt,
    Instant heartbeatAt) {
}