package info.henrycaldwell.streamline.admin.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import info.henrycaldwell.streamline.admin.model.FetchSummary;
import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.repository.FetchFilters;
import info.henrycaldwell.streamline.admin.repository.FetchRepository;
import info.henrycaldwell.streamline.admin.repository.FetchRow;

@Service
public class FetchService {

  private final FetchRepository repository;

  public FetchService(FetchRepository repository) {
    this.repository = repository;
  }

  public Page<FetchSummary> all(String cursor, int limit, FetchFilters filters) {
    Long prevId = decodeCursor(cursor);

    List<FetchRow> rows = repository.all(prevId, limit + 1, filters);

    boolean hasMore = rows.size() > limit;
    if (hasMore) {
      rows = rows.subList(0, limit);
    }

    List<FetchSummary> items = rows.stream().map(this::toSummary).toList();

    String nextCursor = hasMore && !items.isEmpty() ? encodeCursor(items.get(items.size() - 1).id()) : null;

    return new Page<>(items, nextCursor, hasMore);
  }

  private FetchSummary toSummary(FetchRow row) {
    String status = row.status() != null ? row.status() : "in_progress";

    return new FetchSummary(
        row.id(),
        row.runId(),
        row.retriever(),
        status,
        row.error(),
        row.clips(),
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
