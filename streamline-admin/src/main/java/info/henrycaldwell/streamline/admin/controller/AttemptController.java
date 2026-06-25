package info.henrycaldwell.streamline.admin.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import info.henrycaldwell.streamline.admin.model.AttemptSummary;
import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.repository.AttemptFilters;
import info.henrycaldwell.streamline.admin.service.AttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/attempts")
@Tag(name = "Attempts")
public class AttemptController {

  private static final String DEFAULT_LIMIT = "20";
  private static final int MAX_LIMIT = 200;

  private final AttemptService service;

  public AttemptController(AttemptService service) {
    this.service = service;
  }

  @Operation(summary = "List attempts")
  @GetMapping
  public Page<AttemptSummary> all(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) int limit,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long runId,
      @RequestParam(required = false) String clipId,
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    AttemptFilters filters = new AttemptFilters(status, runId, clipId, stage, from, to);

    return service.all(cursor, safeLimit, filters);
  }
}
