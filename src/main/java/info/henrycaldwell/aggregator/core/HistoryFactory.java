package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.history.History;
import info.henrycaldwell.aggregator.history.SqliteHistory;

/**
 * Factory for constructing histories from configuration.
 * 
 * This class validates a history configuration block and instantiates a
 * concrete history implementation.
 */
public final class HistoryFactory {

  private HistoryFactory() {
  }

  /**
   * Builds a history from the given configuration block.
   *
   * @param config A {@link Config} representing the history configuration.
   * @return A {@link History} representing the configured history.
   * @throws SpecException if the configuration is invalid or the history type is
   *                       unknown.
   */
  public static History fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_HISTORY", "Missing required key", Map.of("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", Map.of("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "sqlite" -> {
        return new SqliteHistory(config);
      }
      default -> throw new SpecException(name, "Unknown history type", Map.of("type", type));
    }
  }
}
