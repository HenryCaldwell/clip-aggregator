package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.retrieve.AbstractRetriever;
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

  private static final Map<String, Entry> REGISTRY = Map.of(
      "twitch", new Entry(TwitchRetriever.SPEC, TwitchRetriever::new),
      "no_op", new Entry(NoOpRetriever.SPEC, NoOpRetriever::new));

  private RetrieverFactory() {
  }

  /**
   * Validates a retriever configuration block.
   *
   * @param config A {@link Config} representing the retriever configuration.
   * @param index  An integer representing the retriever index.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config, int index) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_RETRIEVER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = entry != null ? Spec.union(AbstractRetriever.BASE_SPEC, entry.spec())
        : AbstractRetriever.BASE_SPEC;

    List<SpecException> exceptions = composite.validate(config, Retriever.TYPE, null, name, index);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Retriever.TYPE, null, name, "Unknown retriever type",
          index >= 0 ? MapUtils.ofNullable("index", index, "type", type) : MapUtils.ofNullable("type", type)));
    }

    return exceptions;
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
    List<SpecException> exceptions = validate(config, index);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Spec spec, Function<Config, Retriever> factory) {
  }
}
