package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.observe.AbstractObserver;
import info.henrycaldwell.streamline.observe.NoOpObserver;
import info.henrycaldwell.streamline.observe.Observer;
import info.henrycaldwell.streamline.observe.SqliteObserver;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing observers from configuration.
 * 
 * This class validates an observer configuration block and instantiates a
 * concrete observer implementation.
 */
public final class ObserverFactory {

  private static final Map<String, Entry> REGISTRY = Map.of(
      "sqlite", new Entry(SqliteObserver.class, SqliteObserver::new),
      "no_op", new Entry(NoOpObserver.class, NoOpObserver::new));

  private ObserverFactory() {
  }

  /**
   * Validates an observer configuration block.
   *
   * @param config A {@link Config} representing the observer configuration.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_OBSERVER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = Spec.collect(entry != null ? entry.clazz() : AbstractObserver.class);

    List<SpecException> exceptions = composite.validate(config, Observer.TYPE, null, name);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Observer.TYPE, null, name, "Unknown observer type",
          MapUtils.ofNullable("type", type)));
    }

    return exceptions;
  }

  /**
   * Builds an observer from the given configuration block.
   *
   * @param config A {@link Config} representing the observer configuration.
   * @return An {@link Observer} representing the configured observer.
   * @throws SpecException if the configuration is invalid or the observer type is
   *                       unknown.
   */
  public static Observer fromConfig(Config config) {
    List<SpecException> exceptions = validate(config);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Class<? extends Observer> clazz, Function<Config, Observer> factory) {
  }
}
