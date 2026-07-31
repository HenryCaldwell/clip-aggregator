package info.henrycaldwell.streamline.core;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.transform.FpsTransformer;
import info.henrycaldwell.streamline.transform.MusicTransformer;
import info.henrycaldwell.streamline.transform.NoOpTransformer;
import info.henrycaldwell.streamline.transform.TextTransformer;
import info.henrycaldwell.streamline.transform.TitleTransformer;
import info.henrycaldwell.streamline.transform.Transformer;
import info.henrycaldwell.streamline.transform.VerticalBlurTransformer;
import info.henrycaldwell.streamline.transform.WatermarkTransformer;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Factory for constructing transformers from configuration.
 *
 * This class validates a transformer configuration block and instantiates a
 * concrete transformer implementation.
 */
public final class TransformerFactory {

  private TransformerFactory() {
  }

  /**
   * Builds a transformer from the given configuration block.
   *
   * @param config   A {@link Config} representing the transformer configuration.
   * @param pipeline A string representing the parent pipeline name.
   * @param index    An integer representing the transformer index.
   * @return A {@link Transformer} representing the configured transformer.
   * @throws SpecException if the configuration is invalid or the transformer type
   *                       is unknown.
   */
  public static Transformer fromConfig(Config config, String pipeline, int index) {
    if (!config.hasPath("name") || config.getString("name").isBlank()) {
      throw new SpecException(Transformer.TYPE, pipeline, "UNNAMED_TRANSFORMER", "Missing required key",
          MapUtils.ofNullable("index", index, "key", "name"));
    }

    String name = config.getString("name");

    if (!config.hasPath("type") || config.getString("type").isBlank()) {
      throw new SpecException(Transformer.TYPE, pipeline, name, "Missing required key",
          MapUtils.ofNullable("index", index, "key", "type"));
    }

    String type = config.getString("type");

    switch (type) {
      case "vertical_blur" -> {
        return new VerticalBlurTransformer(config);
      }
      case "fps" -> {
        return new FpsTransformer(config);
      }
      case "watermark" -> {
        return new WatermarkTransformer(config);
      }
      case "music" -> {
        return new MusicTransformer(config);
      }
      case "title" -> {
        return new TitleTransformer(config);
      }
      case "text" -> {
        return new TextTransformer(config);
      }
      case "no_op" -> {
        return new NoOpTransformer(config);
      }
      default ->
        throw new SpecException(Transformer.TYPE, pipeline, name, "Unknown transformer type",
            MapUtils.ofNullable("index", index, "type", type));
    }
  }
}
