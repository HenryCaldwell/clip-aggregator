package info.henrycaldwell.streamline.observe;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;

/**
 * Base class for observers that parses common configuration.
 * 
 * This class validates observer configuration using a shared base spec combined
 * with subclass-specific requirements.
 */
public abstract class AbstractObserver implements Observer {

  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .build();

  protected final String name;

  /**
   * Constructs an abstract observer.
   *
   * @param config A {@link Config} representing the observer block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractObserver(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_OBSERVER";

    composite.validate(config, Observer.TYPE, null, display);

    this.name = config.getString("name");
  }

  /**
   * Initializes any underlying resources required by the observer.
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
   * Returns the configured observer name.
   *
   * @return A string representing the observer name.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Records a heartbeat for a live run.
   * 
   * @param runId A long representing the run identifier.
   */
  @Override
  public void heartbeat(long runId) {
    // No-op by default
  }

}
