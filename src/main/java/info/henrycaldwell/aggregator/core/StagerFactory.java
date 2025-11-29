package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.stage.CloudflareR2Stager;
import info.henrycaldwell.aggregator.stage.Stager;

/**
 * Class for constructing stagers from HOCON configuration blocks.
 * 
 * This class validates a stager block using its spec and instantiates
 * a concrete stager.
 */
public final class StagerFactory {

  private StagerFactory() {
  }

  /**
   * Builds a stager from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single stager block.
   * @return A {@link Stager} representing the stager.
   * @throws SpecException if the configuration is missing required fields or the
   *                       type is unknown.
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
