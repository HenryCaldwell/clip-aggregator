package info.henrycaldwell.streamline.admin.repository;

import java.time.Instant;

public record RunFilters(
    String status,
    String runner,
    Instant from,
    Instant to) {
}
