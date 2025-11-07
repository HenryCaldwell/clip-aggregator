package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.publish.Publisher;

/**
 * Class for constructing publishers from HOCON configuration blocks.
 * 
 * This class validates a publisher block using its spec and instantiates
 * a concrete publisher.
 */
public final class PublisherFactory {

  private static final Spec PUBLISHER_BLOCK_SPEC = Spec.builder()
      .requiredString("type", "name")
      .build();

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
    PUBLISHER_BLOCK_SPEC.validate(config, "BASE_PUBLISHER");

    String type = config.getString("type");
    String name = config.getString("name");

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
