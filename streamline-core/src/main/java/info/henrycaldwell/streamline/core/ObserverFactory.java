package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.observe.NoOpObserver;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.SqliteObserver;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing observers from configuration.
 * 
 * This class validates an observer configuration block and instantiates a
 * concrete observer implementation.
 */
public final class ObserverFactory {

  private ObserverFactory() {
  }

  /**
   * Builds an observer from the given configuration block.
   *
   * @param config A {@link Config} representing the observer configuration.
   * @return An {@link Observer} representing the configured observer.
   * @throws SpecException if the configuration is invalid or the observer type is
   *                       unknown.
   */
  public static Observer fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_OBSERVER", "Missing required key", MapUtils.ofNullable("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", MapUtils.ofNullable("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "sqlite" -> {
        return new SqliteObserver(config);
      }
      case "no_op" -> {
        return new NoOpObserver(config);
      }
      default -> throw new SpecException(name, "Unknown observer type", MapUtils.ofNullable("type", type));
    }
  }
}
