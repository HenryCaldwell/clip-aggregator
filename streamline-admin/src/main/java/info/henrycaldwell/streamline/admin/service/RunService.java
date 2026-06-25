package info.henrycaldwell.streamline.admin.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import info.henrycaldwell.streamline.admin.model.FetchSummary;
import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.model.RunSummary;
import info.henrycaldwell.streamline.admin.repository.FetchRepository;
import info.henrycaldwell.streamline.admin.repository.FetchRow;
import info.henrycaldwell.streamline.admin.repository.RunFilters;
import info.henrycaldwell.streamline.admin.repository.RunRepository;
import info.henrycaldwell.streamline.admin.repository.RunRow;

@Service
public class RunService {

  private final RunRepository runRepository;
  private final FetchRepository fetchRepository;
  private final Duration strandedThreshold;

  public RunService(RunRepository runRepository, FetchRepository fetchRepository,
      @Value("${streamline.observer.stranded-threshold}") long strandedThreshold) {
    this.runRepository = runRepository;
    this.fetchRepository = fetchRepository;
    this.strandedThreshold = Duration.ofSeconds(strandedThreshold);
  }

  public Page<RunSummary> all(String cursor, int limit, RunFilters filters) {
    Long prevId = decodeCursor(cursor);

    List<RunRow> rows = runRepository.all(prevId, limit + 1, filters);

    boolean hasMore = rows.size() > limit;
    if (hasMore) {
      rows = rows.subList(0, limit);
    }

    List<RunSummary> items = rows.stream().map(this::toSummary).toList();

    String nextCursor = hasMore && !items.isEmpty() ? encodeCursor(items.get(items.size() - 1).id()) : null;

    return new Page<>(items, nextCursor, hasMore);
  }

  public RunSummary one(long id) {
    RunRow row = runRepository.one(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));

    return toSummary(row);
  }

  public Page<FetchSummary> fetches(long id, String cursor, int limit) {
    if (!runRepository.exists(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found");
    }

    Long prevId = decodeCursor(cursor);

    List<FetchRow> rows = fetchRepository.byRunId(id, prevId, limit + 1);

    boolean hasMore = rows.size() > limit;
    if (hasMore) {
      rows = rows.subList(0, limit);
    }

    List<FetchSummary> items = rows.stream().map(this::toFetchSummary).toList();

    String nextCursor = hasMore && !items.isEmpty() ? encodeCursor(items.get(items.size() - 1).id()) : null;

    return new Page<>(items, nextCursor, hasMore);
  }

  private RunSummary toSummary(RunRow row) {
    String status;
    if (row.status() != null) {
      status = row.status();
    } else if (row.heartbeatAt() != null
        && Duration.between(row.heartbeatAt(), Instant.now()).compareTo(strandedThreshold) <= 0) {
      status = "in_progress";
    } else {
      status = "stranded";
    }

    return new RunSummary(
        row.id(),
        row.runner(),
        status,
        row.published(),
        row.startedAt(),
        row.endedAt());
  }

  private FetchSummary toFetchSummary(FetchRow row) {
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
