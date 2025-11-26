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
   * Initializes any underlying resources required by the history.
   */
  @Override
  public void start() {
    // No-op by default
  }

  /**
   * Releases any resources acquired by {@link #start()}.
   */
  @Override
  public void stop() {
    // No-op by default
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
