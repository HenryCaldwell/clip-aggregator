package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.retrieve.Retriever;
import info.henrycaldwell.aggregator.retrieve.TwitchRetriever;

/**
 * Factory for constructing retrievers from configuration.
 * 
 * This class validates a retriever configuration block and instantiates a
 * concrete retriever implementation.
 */
public final class RetrieverFactory {

  private RetrieverFactory() {
  }

  /**
   * Builds a retriever from the given configuration block.
   *
   * @param config A {@link Config} representing the retriever configuration.
   * @return A {@link Retriever} representing the configured retriever.
   * @throws SpecException if the configuration is invalid or the retriever type
   *                       is unknown.
   */
  public static Retriever fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_RETRIEVER", "Missing required key", Map.of("key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(name, "Missing required key", Map.of("key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "twitch" -> {
        return new TwitchRetriever(config);
      }
      default -> throw new SpecException(name, "Unknown retriever type", Map.of("type", type));
    }
  }
}
