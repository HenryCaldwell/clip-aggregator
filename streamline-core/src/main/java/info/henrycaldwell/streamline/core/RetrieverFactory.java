package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.retrieve.NoOpRetriever;
import info.henrycaldwell.streamline.retrieve.Retriever;
import info.henrycaldwell.streamline.retrieve.TwitchRetriever;
import info.henrycaldwell.streamline.util.MapUtils;

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
   * @param index  An integer representing the retriever index.
   * @return A {@link Retriever} representing the configured retriever.
   * @throws SpecException if the configuration is invalid or the retriever type
   *                       is unknown.
   */
  public static Retriever fromConfig(Config config, int index) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException(Retriever.TYPE, null, "UNNAMED_RETRIEVER", "Missing required key",
          MapUtils.ofNullable("index", index, "key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(Retriever.TYPE, null, name, "Missing required key",
          MapUtils.ofNullable("index", index, "key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "twitch" -> {
        return new TwitchRetriever(config);
      }
      case "no_op" -> {
        return new NoOpRetriever(config);
      }
      default ->
        throw new SpecException(Retriever.TYPE, null, name, "Unknown retriever type",
            MapUtils.ofNullable("index", index, "type", type));
    }
  }
}
