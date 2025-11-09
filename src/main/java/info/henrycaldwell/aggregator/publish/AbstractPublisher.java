package info.henrycaldwell.aggregator.publish;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Base class for publishers that parses common configuration.
 * 
 * This class reads the required publisher base properties, and validates using
 * a composite Spec of the base keys and the subclass-specific keys.
 */
public abstract class AbstractPublisher implements Publisher {
  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs a base publisher.
   *
   * @param config A {@link Config} representing the publisher block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractPublisher(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_PUBLISHER";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Returns the configured publisher name.
   *
   * @return A string representing the publisher name.
   */
  @Override
  public String getName() {
    return name;
  }
}
