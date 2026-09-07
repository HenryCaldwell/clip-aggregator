package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.stage.AbstractStager;
import info.henrycaldwell.streamline.stage.AwsS3Stager;
import info.henrycaldwell.streamline.stage.CloudflareR2Stager;
import info.henrycaldwell.streamline.stage.NoOpStager;
import info.henrycaldwell.streamline.stage.Stager;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing stagers from configuration.
 * 
 * This class validates a stager configuration block and instantiates a concrete
 * stager implementation.
 */
public final class StagerFactory {

  private static final Map<String, Entry> REGISTRY = Map.of(
      "cloudflare-r2", new Entry(CloudflareR2Stager.class, CloudflareR2Stager::new),
      "aws-s3", new Entry(AwsS3Stager.class, AwsS3Stager::new),
      "no_op", new Entry(NoOpStager.class, NoOpStager::new));

  private StagerFactory() {
  }

  /**
   * Validates a stager configuration block.
   *
   * @param config A {@link Config} representing the stager configuration.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_STAGER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = Spec.collect(entry != null ? entry.clazz() : AbstractStager.class);

    List<SpecException> exceptions = composite.validate(config, Stager.TYPE, null, name);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Stager.TYPE, null, name, "Unknown stager type",
          MapUtils.ofNullable("type", type)));
    }

    return exceptions;
  }

  /**
   * Builds a stager from the given configuration block.
   *
   * @param config A {@link Config} representing the stager configuration.
   * @return A {@link Stager} representing the configured stager.
   * @throws SpecException if the configuration is invalid or the stager type is
   *                       unknown.
   */
  public static Stager fromConfig(Config config) {
    List<SpecException> exceptions = validate(config);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Class<? extends Stager> clazz, Function<Config, Stager> factory) {
  }
}
