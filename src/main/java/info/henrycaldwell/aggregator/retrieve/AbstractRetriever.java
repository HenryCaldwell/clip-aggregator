package info.henrycaldwell.aggregator.retrieve;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Base class for retrievers that parses common configuration.
 * 
 * This class validates retriever configuration using a shared base spec
 * combined with subclass-specific requirements.
 */
public abstract class AbstractRetriever implements Retriever {

  protected static final Spec BASE_SPEC = Spec.builder()
      .requiredString("name", "type")
      .optionalString("pipeline")
      .build();

  protected final String name;
  protected final String pipeline;

  /**
   * Constructs an abstract retriever.
   *
   * @param config A {@link Config} representing the retriever block.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   */
  protected AbstractRetriever(Config config, Spec spec) {
    Spec composite = Spec.union(BASE_SPEC, spec);

    String display = config.hasPath("name") && !config.getString("name").isBlank()
        ? config.getString("name")
        : "UNNAMED_RETRIEVER";

    composite.validate(config, display);

    this.name = config.getString("name");
    this.pipeline = config.hasPath("pipeline") && !config.getString("pipeline").isBlank()
        ? config.getString("pipeline")
        : null;
  }

  /**
   * Returns the configured retriever name.
   *
   * @return A string representing the retriever name.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the configured pipeline name.
   *
   * @return A string representing the pipeline name, or {@code null}.
   */
  @Override
  public String getPipeline() {
    return pipeline;
  }
}
