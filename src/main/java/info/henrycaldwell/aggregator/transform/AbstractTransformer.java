package info.henrycaldwell.aggregator.transform;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Base class for transformers that parses common configuration.
 * 
 * This class reads the required transformer base properties, and validates
 * using a composite Spec of the base keys and the subclass-specific keys.
 */
public abstract class AbstractTransformer implements Transformer {
  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs a base transformer.
   *
   * @param config A {@link Config} representing the transformer block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractTransformer(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_TRANSFORMER";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Returns the configured transformer name.
   *
   * @return A string representing the transformer name.
   */
  @Override
  public String getName() {
    return name;
  }
}
