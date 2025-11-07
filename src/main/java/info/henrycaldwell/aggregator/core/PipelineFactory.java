package info.henrycaldwell.aggregator.core;

import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.transform.Transformer;

/**
 * Class for constructing pipelines from HOCON configuration blocks.
 * 
 * This class validates each transformer block using its spec and instantiates
 * the transformer pipeline.
 */
public final class PipelineFactory {

  private static final Spec PIPELINE_BLOCK_SPEC = Spec.builder()
      .requiredString("name")
      .build();

  private static final Spec TRANSFORMER_BLOCK_SPEC = Spec.builder()
      .requiredString("type")
      .build();

  private PipelineFactory() {
  }

  /**
   * Builds a pipeline from a HOCON configuration block.
   *
   * @param config A {@link Config} representing a single pipeline block.
   * @return A {@link Pipeline} representing an ordered list of transformers.
   * @throws IllegalArgumentException if the type is unknown.
   */
  public static Pipeline fromConfig(Config config) {
    PIPELINE_BLOCK_SPEC.validate(config, "BASE_PIPELINE");

    String name = config.getString("name");
    List<Transformer> steps = new ArrayList<>();

    if (config.hasPath("transformers")) {
      List<? extends Config> transformers = config.getConfigList("transformers");

      for (int i = 0; i < transformers.size(); i++) {
        Config transformerConfig = transformers.get(i);
        String baseContext = name + " BASE_TRANSFORMER[" + i + "]";

        TRANSFORMER_BLOCK_SPEC.validate(transformerConfig, baseContext);

        String type = transformerConfig.getString("type");
        String context = name + " " + type;

        switch (type) {
          case "EXAMPLE 1" -> {
            // FUTURE IMPLEMENTATION VALIDATE EACH
            steps.add(null);
          }
          case "EXAMPLE 2" -> {
            // FUTURE IMPLEMENTATION VALIDATE EACH
            steps.add(null);
          }
          default -> throw new IllegalArgumentException("Unknown transformer type " + type + " (" + name + ")");
        }
      }
    }

    return new Pipeline(steps);
  }
}
