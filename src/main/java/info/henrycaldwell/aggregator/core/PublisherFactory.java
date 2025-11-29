package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.publish.InstagramPublisher;
import info.henrycaldwell.aggregator.publish.NoOpPublisher;
import info.henrycaldwell.aggregator.publish.Publisher;

/**
 * Class for constructing publishers from HOCON configuration blocks.
 * 
 * This class validates a publisher block using its spec and instantiates
 * a concrete publisher.
 */
public final class PublisherFactory {

  private PublisherFactory() {
  }

  /**
   * Builds a publisher from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single publisher block.
   * @return A {@link Publisher} representing the destination.
   * @throws SpecException if the configuration is missing required fields or the
   *                       type is unknown.
   */
  public static Publisher fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_PUBLISHER", "Missing required key", Map.of("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", Map.of("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "instagram" -> {
        return new InstagramPublisher(config);
      }
      case "no_op" -> {
        return new NoOpPublisher(config);
      }
      default -> throw new SpecException(name, "Unknown publisher type", Map.of("type", type));
    }
  }
}
