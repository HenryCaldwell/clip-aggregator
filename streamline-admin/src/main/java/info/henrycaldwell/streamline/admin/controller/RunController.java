package info.henrycaldwell.streamline.admin.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import info.henrycaldwell.streamline.admin.model.Page;
import info.henrycaldwell.streamline.admin.model.RunSummary;
import info.henrycaldwell.streamline.admin.repository.RunFilters;
import info.henrycaldwell.streamline.admin.service.RunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/runs")
@Tag(name = "Runs")
public class RunController {

  private static final String DEFAULT_LIMIT = "20";
  private static final int MAX_LIMIT = 200;

  private final RunService service;

  public RunController(RunService service) {
    this.service = service;
  }

  @Operation(summary = "List runs")
  @GetMapping
  public Page<RunSummary> all(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) int limit,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String runner,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
    RunFilters filters = new RunFilters(status, runner, from, to);

    return service.all(cursor, safeLimit, filters);
  }
}
