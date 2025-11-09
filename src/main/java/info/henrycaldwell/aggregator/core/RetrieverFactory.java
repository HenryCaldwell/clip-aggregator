package info.henrycaldwell.aggregator.core;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.retrieval.Retriever;
import info.henrycaldwell.aggregator.retrieval.TwitchRetriever;

/**
 * Class for constructing retrievers from HOCON configuration blocks.
 * 
 * This class validates a retriever block using its spec and instantiates
 * a concrete retriever.
 */
public final class RetrieverFactory {

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
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new IllegalArgumentException("Missing required key name (UNNAMED_RETRIEVER)");
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new IllegalArgumentException("Missing required key type (" + name + ")");
    }

    String type = config.getString("type");

    switch (type) {
      case "twitch" -> {
        return new TwitchRetriever(config);
      }
      default -> throw new IllegalArgumentException("Unknown retriever type " + type + " (" + name + ")");
    }
  }
}
