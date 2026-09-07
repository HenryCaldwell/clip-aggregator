package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.publish.AbstractPublisher;
import info.henrycaldwell.streamline.publish.InstagramPublisher;
import info.henrycaldwell.streamline.publish.NoOpPublisher;
import info.henrycaldwell.streamline.publish.Publisher;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing publishers from configuration.
 * 
 * This class validates a publisher configuration block and instantiates a
 * concrete publisher implementation.
 */
public final class PublisherFactory {

  private static final Map<String, Entry> REGISTRY = Map.of(
      "instagram", new Entry(InstagramPublisher.class, InstagramPublisher::new),
      "no_op", new Entry(NoOpPublisher.class, NoOpPublisher::new));

  private PublisherFactory() {
  }

  /**
   * Validates a publisher configuration block.
   *
   * @param config A {@link Config} representing the publisher configuration.
   * @param index  An integer representing the publisher index.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config, int index) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_PUBLISHER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = Spec.collect(entry != null ? entry.clazz() : AbstractPublisher.class);

    List<SpecException> exceptions = composite.validate(config, Publisher.TYPE, null, name, index);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Publisher.TYPE, null, name, "Unknown publisher type",
          index >= 0 ? MapUtils.ofNullable("index", index, "type", type) : MapUtils.ofNullable("type", type)));
    }

    return exceptions;
  }

  /**
   * Builds a publisher from the given configuration block.
   *
   * @param config A {@link Config} representing the publisher configuration.
   * @param index  An integer representing the publisher index.
   * @return A {@link Publisher} representing the configured publisher.
   * @throws SpecException if the configuration is invalid or the publisher type
   *                       is unknown.
   */
  public static Publisher fromConfig(Config config, int index) {
    List<SpecException> exceptions = validate(config, index);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Class<? extends Publisher> clazz, Function<Config, Publisher> factory) {
  }
}
