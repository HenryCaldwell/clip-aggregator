package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

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
   * @throws IllegalArgumentException if the type is unknown.
   */
  public static Publisher fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new IllegalArgumentException("Missing required key name (UNNAMED_PUBLISHER)");
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new IllegalArgumentException("Missing required key type (" + name + ")");
    }

    String type = config.getString("type");

    switch (type) {
      case "EXAMPLE 1" -> {
        // FUTURE IMPLEMENTATION VALIDATE EACH
        return null;
      }
      case "EXAMPLE 2" -> {
        // FUTURE IMPLEMENTATION VALIDATE EACH
        return null;
      }
      default -> throw new IllegalArgumentException("Unknown publisher type " + type + " (" + name + ")");
    }
  }
}
