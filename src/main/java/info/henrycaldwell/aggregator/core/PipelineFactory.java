package info.henrycaldwell.aggregator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import info.henrycaldwell.aggregator.error.SpecException;
import info.henrycaldwell.aggregator.transform.FpsTransformer;
import info.henrycaldwell.aggregator.transform.MusicTransformer;
import info.henrycaldwell.aggregator.transform.Pipeline;
import info.henrycaldwell.aggregator.transform.TitleTransformer;
import info.henrycaldwell.aggregator.transform.Transformer;
import info.henrycaldwell.aggregator.transform.VerticalBlurTransformer;
import info.henrycaldwell.aggregator.transform.WatermarkTransformer;

/**
 * Factory for constructing pipelines from configuration.
 * 
 * This class validates transformer configuration blocks and assembles them into
 * an ordered pipeline.
 */
public final class PipelineFactory {

  private PipelineFactory() {
  }

  /**
   * Builds a pipeline from the given configuration block.
   *
   * @param config A {@link Config} representing the pipeline configuration.
   * @return A {@link Pipeline} representing the configured transformer pipeline.
   * @throws SpecException if the configuration is invalid or any transformer type
   *                       is unknown.
   */
  public static Pipeline fromConfig(Config config) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException("UNNAMED_PIPELINE", "Missing required key", Map.of("key", "name"));
    }

    String pipelineName = config.getString("name");

    if (!config.hasPath("transformers")) {
      throw new SpecException(pipelineName, "Missing required key", Map.of("key", "transformers"));
    }

    List<? extends Config> transformers;
    try {
      transformers = config.getConfigList("transformers");
    } catch (ConfigException.WrongType e) {
      throw new SpecException(pipelineName, "Incorrect key type (expected list)", Map.of("key", "transformers"), e);
    }

    List<Transformer> steps = new ArrayList<>();

    for (int i = 0; i < transformers.size(); i++) {
      Config transformerConfig = transformers.get(i);

      if (!transformerConfig.hasPath("name") || transformerConfig.getString("name").isBlank()) {
        throw new SpecException("UNNAMED_TRANSFORMER", "Missing required key", Map.of("key", "name"));
      }

      String transformerName = transformerConfig.getString("name");

      if (!transformerConfig.hasPath("type") || transformerConfig.getString("type").isBlank()) {
        throw new SpecException(transformerName, "Missing required key", Map.of("key", "type"));
      }

      String type = transformerConfig.getString("type");

      switch (type) {
        case "vertical_blur" -> {
          steps.add(new VerticalBlurTransformer(transformerConfig));
        }
        case "fps" -> {
          steps.add(new FpsTransformer(transformerConfig));
        }
        case "watermark" -> {
          steps.add(new WatermarkTransformer(transformerConfig));
        }
        case "music" -> {
          steps.add(new MusicTransformer(transformerConfig));
        }
        case "title" -> {
          steps.add(new TitleTransformer(transformerConfig));
        }
        default -> throw new SpecException(transformerName, "Unknown transformer type", Map.of("type", type));
      }
    }

    return new Pipeline(pipelineName, steps);
  }
}
