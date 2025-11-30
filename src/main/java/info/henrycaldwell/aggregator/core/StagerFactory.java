package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.stage.CloudflareR2Stager;
import info.henrycaldwell.aggregator.stage.Stager;

/**
 * Factory for constructing stagers from configuration.
 * 
 * This class validates a stager configuration block and instantiates a concrete
 * stager implementation.
 */
public final class StagerFactory {

  private StagerFactory() {
  }

  /**
   * Builds a stager from the given configuration block.
   *
   * @param config A {@link Config} representing the stager configuration.
   * @return A {@link Stager} representing the configured stager.
   * @throws SpecException if the configuration is invalid or the stager type is
   *                       unknown.
   */
  public static Stager fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_STAGER", "Missing required key", Map.of("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", Map.of("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "cloudflare-r2" -> {
        return new CloudflareR2Stager(config);
      }
      default -> throw new SpecException(name, "Unknown stager type", Map.of("type", type));
    }
  }
}
