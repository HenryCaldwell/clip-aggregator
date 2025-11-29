package info.henrycaldwell.aggregator.core;

import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.retrieve.Retriever;
import info.henrycaldwell.aggregator.retrieve.TwitchRetriever;

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
   * @throws SpecException if the configuration is missing required fields or the
   *                       type is unknown.
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
