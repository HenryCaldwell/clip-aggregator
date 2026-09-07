package info.henrycaldwell.streamline.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.transform.Pipeline;
import info.henrycaldwell.streamline.transform.Transformer;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing pipelines from configuration.
 *
 * This class validates a pipeline configuration block and assembles the
 * configured transformers into an ordered pipeline.
 */
public final class PipelineFactory {

  private PipelineFactory() {
  }

  /**
   * Validates a pipeline configuration block.
   *
   * @param config A {@link Config} representing the pipeline configuration.
   * @param index  An integer representing the pipeline index.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config, int index) {
    String pipelineName = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_PIPELINE";

    List<SpecException> exceptions = Pipeline.SPEC.validate(config, Pipeline.TYPE, null, pipelineName, index);

    if (!config.hasPath("transformers")) {
      return exceptions;
    }

    List<? extends Config> configs;
    try {
      configs = config.getConfigList("transformers");
    } catch (ConfigException.WrongType e) {
      return exceptions;
    }

    Set<String> seenNames = new HashSet<>();
    for (int i = 0; i < configs.size(); i++) {
      Config transformerConfig = configs.get(i);
      exceptions.addAll(TransformerFactory.validate(transformerConfig, pipelineName, i));

      if (transformerConfig.hasPath("name") && !transformerConfig.getString("name").isBlank()) {
        String transformerName = transformerConfig.getString("name");

        if (!seenNames.add(transformerName)) {
          exceptions.add(new SpecException(Transformer.TYPE, pipelineName, transformerName, "Duplicate transformer name",
              MapUtils.ofNullable("index", i, "name", transformerName)));
        }
      }
    }

    return exceptions;
  }

  /**
   * Builds a pipeline from the given configuration block.
   *
   * @param config A {@link Config} representing the pipeline configuration.
   * @param index  An integer representing the pipeline index.
   * @return A {@link Pipeline} representing the configured pipeline.
   * @throws SpecException if the configuration is invalid or any transformer type
   *                       is unknown.
   */
  public static Pipeline fromConfig(Config config, int index) {
    List<SpecException> exceptions = validate(config, index);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    String pipelineName = config.getString("name");
    List<? extends Config> configs = config.getConfigList("transformers");
    Map<String, Transformer> transformers = new LinkedHashMap<>();

    for (int i = 0; i < configs.size(); i++) {
      Transformer transformer = TransformerFactory.fromConfig(configs.get(i), pipelineName, i);
      transformers.put(transformer.getName(), transformer);
    }

    return new Pipeline(pipelineName, new ArrayList<>(transformers.values()));
  }
}
