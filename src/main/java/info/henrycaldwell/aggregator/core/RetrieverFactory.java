package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.retrieval.Retriever;

/**
 * Class for constructing retrievers from HOCON configuration blocks.
 * 
 * This class validates a retriever block using its spec and instantiates
 * a concrete retriever.
 */
public final class RetrieverFactory {

  private static final Spec RETRIEVER_BLOCK_SPEC = Spec.builder()
      .requiredString("type", "name")
      .build();

  private RetrieverFactory() {
  }

  /**
   * Builds a retriever from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single retriever block.
   * @return A {@link Retriever} representing the source.
   * @throws IllegalArgumentException if the type is unknown.
   */
  public static Retriever fromConfig(Config config) {
    RETRIEVER_BLOCK_SPEC.validate(config, "BASE_RETRIEVER");

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
      default -> throw new IllegalArgumentException("Unknown retriever type " + type + " (" + name + ")");
    }
  }
}
