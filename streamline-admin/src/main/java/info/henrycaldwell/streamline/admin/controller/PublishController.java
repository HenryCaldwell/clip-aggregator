package info.henrycaldwell.streamline.admin.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.model.PublishSummary;
import info.henrycaldwell.streamline.admin.repository.PublishFilters;
import info.henrycaldwell.streamline.admin.service.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/publishes")
@Tag(name = "Publishes")
public class PublishController {

  private static final String DEFAULT_LIMIT = "20";
  private static final int MAX_LIMIT = 200;

  private final PublishService service;

  public PublishController(PublishService service) {
    this.service = service;
  }

  @Operation(summary = "List publishes")
  @GetMapping
  public Page<PublishSummary> all(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) int limit,
      @RequestParam(required = false) String publisher,
      @RequestParam(required = false) Long runId,
      @RequestParam(required = false) String clipId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    PublishFilters filters = new PublishFilters(publisher, runId, clipId, from, to);

    return service.all(cursor, safeLimit, filters);
  }
}
