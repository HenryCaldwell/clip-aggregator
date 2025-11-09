package info.henrycaldwell.aggregator.core;

import java.util.ArrayList;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import info.henrycaldwell.aggregator.transform.FpsTransformer;
import info.henrycaldwell.aggregator.transform.Transformer;
import info.henrycaldwell.aggregator.transform.VerticalBlurTransformer;

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

    String pipelineName = config.getString("name");

    if (!config.hasPath("transformers")) {
      throw new IllegalArgumentException("Missing required key transformers (" + pipelineName + ")");
    }

    List<Transformer> steps = new ArrayList<>();

    if (config.hasPath("transformers")) {
      List<? extends Config> transformers;
      try {
        transformers = config.getConfigList("transformers");
      } catch (ConfigException.WrongType e) {
        throw new IllegalArgumentException("Invalid type for key transformers (" + pipelineName + ")", e);
      }

      for (int i = 0; i < transformers.size(); i++) {
        Config transformerConfig = transformers.get(i);

        if (!config.hasPath("name") || config.getString("name").isBlank()) {
          throw new IllegalArgumentException("Missing required key name (UNNAMED_TRANSFORMER)");
        }

        String transformerName = config.getString("name");

        if (!transformerConfig.hasPath("type") || transformerConfig.getString("type").isBlank()) {
          throw new IllegalArgumentException("Missing required key type (" + transformerName + ")");
        }

        String type = transformerConfig.getString("type");

        switch (type) {
          case "vertical_blur" -> {
            steps.add(new VerticalBlurTransformer(transformerConfig));
          }
          case "fps" -> {
            steps.add(new FpsTransformer(transformerConfig));
          }
          default ->
            throw new IllegalArgumentException("Unknown transformer type " + type + " (" + transformerName + ")");
        }
      }
    }

    return new Pipeline(pipelineName, steps);
  }
}
