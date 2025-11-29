package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.history.History;
import info.henrycaldwell.aggregator.history.SqliteHistory;

/**
 * Class for constructing histories from HOCON configuration blocks.
 * 
 * This class validates a history block using its spec and instantiates
 * a concrete history.
 */
public final class HistoryFactory {

  private HistoryFactory() {
  }

  /**
   * Builds a history from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single history block.
   * @return A {@link History} representing the history.
   * @throws SpecException if the configuration is missing required fields or the
   *                       type is unknown.
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
