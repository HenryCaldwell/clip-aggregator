package info.henrycaldwell.streamline.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
   * Builds a pipeline from the given configuration block.
   *
   * @param config A {@link Config} representing the pipeline configuration.
   * @param index  An integer representing the pipeline index.
   * @return A {@link Pipeline} representing the configured pipeline.
   * @throws SpecException if the configuration is invalid or any transformer type
   *                       is unknown.
   */
  public static Pipeline fromConfig(Config config, int index) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException(Pipeline.TYPE, null, "UNNAMED_PIPELINE", "Missing required key",
          MapUtils.ofNullable("index", index, "key", "name"));
    }

    String pipelineName = config.getString("name");

    if (!config.hasPath("transformers")) {
      throw new SpecException(Pipeline.TYPE, null, pipelineName, "Missing required key",
          MapUtils.ofNullable("index", index, "key", "transformers"));
    }

    List<? extends Config> configs;
    try {
      configs = config.getConfigList("transformers");
    } catch (ConfigException.WrongType e) {
      throw new SpecException(Pipeline.TYPE, null, pipelineName, "Incorrect key type (expected list)",
          MapUtils.ofNullable("index", index, "key", "transformers"), e);
    }

    Map<String, Transformer> transformers = new LinkedHashMap<>();

    for (int i = 0; i < configs.size(); i++) {
      Transformer transformer = TransformerFactory.fromConfig(configs.get(i), pipelineName, i);
      String name = transformer.getName();

      if (transformers.containsKey(name)) {
        throw new SpecException(Transformer.TYPE, pipelineName, name, "Duplicate transformer name",
            MapUtils.ofNullable("index", i, "name", name));
      }

      transformers.put(name, transformer);
    }

    return new Pipeline(pipelineName, new ArrayList<>(transformers.values()));
  }
}
