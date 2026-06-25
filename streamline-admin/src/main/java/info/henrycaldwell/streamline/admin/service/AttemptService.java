package info.henrycaldwell.streamline.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import info.henrycaldwell.streamline.admin.model.AttemptSummary;
import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.repository.AttemptFilters;
import info.henrycaldwell.streamline.admin.repository.AttemptRepository;
import info.henrycaldwell.streamline.admin.repository.AttemptRow;

@Service
public class AttemptService {

  private final AttemptRepository repository;

  public AttemptService(AttemptRepository repository) {
    this.repository = repository;
  }

  public Page<AttemptSummary> all(String cursor, int limit, AttemptFilters filters) {
    Long prevId = decodeCursor(cursor);

    List<AttemptRow> rows = repository.all(prevId, limit + 1, filters);

    boolean hasMore = rows.size() > limit;
    if (hasMore) {
      rows = rows.subList(0, limit);
    }

    List<AttemptSummary> items = rows.stream().map(this::toSummary).toList();

    String nextCursor = hasMore && !items.isEmpty() ? encodeCursor(items.get(items.size() - 1).id()) : null;

    return new Page<>(items, nextCursor, hasMore);
  }

  public AttemptSummary one(long id) {
    AttemptRow row = repository.one(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found"));

    return toSummary(row);
  }

  private AttemptSummary toSummary(AttemptRow row) {
    String status = row.status() != null ? row.status() : "in_progress";

    return new AttemptSummary(
        row.id(),
        row.runId(),
        row.worker(),
        row.clipId(),
        row.stage(),
        row.component(),
        status,
        row.error(),
        row.startedAt(),
        row.endedAt());
  }

  private String encodeCursor(long id) {
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(Long.toString(id).getBytes(StandardCharsets.UTF_8));
  }

  private Long decodeCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);

      return Long.parseLong(decoded);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
    }
  }
}
