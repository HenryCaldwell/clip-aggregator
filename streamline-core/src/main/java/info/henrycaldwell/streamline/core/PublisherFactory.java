package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.publish.InstagramPublisher;
import info.henrycaldwell.streamline.publish.NoOpPublisher;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.util.MapUtils;

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
   * @param index  An integer representing the publisher index.
   * @return A {@link Publisher} representing the configured publisher.
   * @throws SpecException if the configuration is invalid or the publisher type
   *                       is unknown.
   */
  public static Publisher fromConfig(Config config, int index) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException(Publisher.TYPE, null, "UNNAMED_PUBLISHER", "Missing required key",
          MapUtils.ofNullable("index", index, "key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(Publisher.TYPE, null, name, "Missing required key",
          MapUtils.ofNullable("index", index, "key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "instagram" -> {
        return new InstagramPublisher(config);
      }
      case "no_op" -> {
        return new NoOpPublisher(config);
      }
      default ->
        throw new SpecException(Publisher.TYPE, null, name, "Unknown publisher type",
            MapUtils.ofNullable("index", index, "type", type));
    }
  }
}
