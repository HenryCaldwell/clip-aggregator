package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.publish.InstagramPublisher;
import info.henrycaldwell.aggregator.publish.NoOpPublisher;
import info.henrycaldwell.aggregator.publish.Publisher;

/**
 * Factory for constructing publishers from configuration.
 * 
 * This class validates a publisher configuration block and instantiates a
 * concrete publisher implementation.
 */
public final class PublisherFactory {

  private PublisherFactory() {
  }

  /**
   * Builds a publisher from the given configuration block.
   *
   * @param config A {@link Config} representing the publisher configuration.
   * @return A {@link Publisher} representing the configured publisher.
   * @throws SpecException if the configuration is invalid or the publisher type
   *                       is unknown.
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
