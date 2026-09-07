package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.history.AbstractHistory;
import info.henrycaldwell.streamline.history.History;
import info.henrycaldwell.streamline.history.NoOpHistory;
import info.henrycaldwell.streamline.history.SqliteHistory;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing histories from configuration.
 * 
 * This class validates a history configuration block and instantiates a
 * concrete history implementation.
 */
public final class HistoryFactory {

  private static final Map<String, Entry> REGISTRY = Map.of(
      "sqlite", new Entry(SqliteHistory.class, SqliteHistory::new),
      "no_op", new Entry(NoOpHistory.class, NoOpHistory::new));

  private HistoryFactory() {
  }

  /**
   * Validates a history configuration block.
   *
   * @param config A {@link Config} representing the history configuration.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_HISTORY";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = Spec.collect(entry != null ? entry.clazz() : AbstractHistory.class);

    List<SpecException> exceptions = composite.validate(config, History.TYPE, null, name);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(History.TYPE, null, name, "Unknown history type",
          MapUtils.ofNullable("type", type)));
    }

    return exceptions;
  }

  /**
   * Builds a history from the given configuration block.
   *
   * @param config A {@link Config} representing the history configuration.
   * @return A {@link History} representing the configured history.
   * @throws SpecException if the configuration is invalid or the history type is
   *                       unknown.
   */
  public static History fromConfig(Config config) {
    List<SpecException> exceptions = validate(config);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Class<? extends History> clazz, Function<Config, History> factory) {
  }
}
