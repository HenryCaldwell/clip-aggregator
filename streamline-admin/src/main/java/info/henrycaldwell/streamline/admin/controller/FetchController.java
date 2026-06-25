package info.henrycaldwell.streamline.admin.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import info.henrycaldwell.streamline.admin.model.FetchSummary;
import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.repository.FetchFilters;
import info.henrycaldwell.streamline.admin.service.FetchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/fetches")
@Tag(name = "Fetches")
public class FetchController {

  private static final String DEFAULT_LIMIT = "20";
  private static final int MAX_LIMIT = 200;

  private final FetchService service;

  public FetchController(FetchService service) {
    this.service = service;
  }

  @Operation(summary = "List fetches")
  @GetMapping
  public Page<FetchSummary> all(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) int limit,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String retriever,
      @RequestParam(required = false) Long runId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    FetchFilters filters = new FetchFilters(status, retriever, runId, from, to);

    return service.all(cursor, safeLimit, filters);
  }

  @Operation(summary = "Get fetch")
  @GetMapping("/{id}")
  public FetchSummary one(@PathVariable long id) {
    return service.one(id);
  }
}
