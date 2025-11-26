package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

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
   * @throws IllegalArgumentException if the type is unknown.
   */
  public static History fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new IllegalArgumentException("Missing required key name (UNNAMED_HISTORY)");
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new IllegalArgumentException("Missing required key type (" + name + ")");
    }

    String type = config.getString("type");

    switch (type) {
      case "sqlite" -> {
        return new SqliteHistory(config);
      }
      default -> throw new IllegalArgumentException("Unknown history type " + type + " (" + name + ")");
    }
  }
}
