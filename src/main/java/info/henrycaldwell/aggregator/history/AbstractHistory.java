package info.henrycaldwell.aggregator.history;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Base class for histories that parses common configuration.
 * 
 * This class reads the required history base properties, and validates using
 * a composite Spec of the base keys and the subclass-specific keys.
 */
public abstract class AbstractHistory implements History {

  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs a base history.
   *
   * @param config A {@link Config} representing the history block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractHistory(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_HISTORY";

    composite.validate(config, display);

    this.name = config.getString("name");
  }

  /**
   * Returns the configured history name.
   *
   * @return A string representing the history name.
   */
  @Override
  public String getName() {
    return name;
  }
}
