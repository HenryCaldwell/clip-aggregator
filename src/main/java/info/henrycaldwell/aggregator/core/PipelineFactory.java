package info.henrycaldwell.aggregator.core;

import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import info.henrycaldwell.aggregator.transform.Transformer;

/**
 * Class for constructing pipelines from HOCON configuration blocks.
 * 
 * This class validates each transformer block using its spec and instantiates
 * the transformer pipeline.
 */
public final class PipelineFactory {

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
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new IllegalArgumentException("Missing required key name (UNNAMED_PIPELINE)");
    }

    String name = config.getString("name");

    if (!config.hasPath("transformers")) {
      throw new IllegalArgumentException("Missing required key transformers (" + name + ")");
    }

    List<Transformer> steps = new ArrayList<>();

    if (config.hasPath("transformers")) {
      List<? extends Config> transformers;
      try {
        transformers = config.getConfigList("transformers");
      } catch (ConfigException.WrongType e) {
        throw new IllegalArgumentException("Invalid type for key transformers (" + name + ")", e);
      }

      for (int i = 0; i < transformers.size(); i++) {
        Config transformerConfig = transformers.get(i);
        String baseContext = name + " BASE_TRANSFORMER[" + i + "]";

        if (!transformerConfig.hasPath("type") || transformerConfig.getString("type").isBlank()) {
          throw new IllegalArgumentException("Missing required key type (" + baseContext + ")");
        }

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
