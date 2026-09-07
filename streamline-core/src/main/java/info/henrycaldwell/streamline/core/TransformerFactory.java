package info.henrycaldwell.streamline.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.transform.AbstractTransformer;
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

  private static final Map<String, Entry> REGISTRY = Map.of(
      "vertical_blur", new Entry(VerticalBlurTransformer.class, VerticalBlurTransformer::new),
      "fps", new Entry(FpsTransformer.class, FpsTransformer::new),
      "watermark", new Entry(WatermarkTransformer.class, WatermarkTransformer::new),
      "music", new Entry(MusicTransformer.class, MusicTransformer::new),
      "title", new Entry(TitleTransformer.class, TitleTransformer::new),
      "text", new Entry(TextTransformer.class, TextTransformer::new),
      "no_op", new Entry(NoOpTransformer.class, NoOpTransformer::new));

  private TransformerFactory() {
  }

  /**
   * Validates a transformer configuration block.
   *
   * @param config   A {@link Config} representing the transformer configuration.
   * @param pipeline A string representing the parent pipeline name.
   * @param index    An integer representing the transformer index.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public static List<SpecException> validate(Config config, String pipeline, int index) {
    String name = config.hasPath("name") && !config.getString("name").isBlank() ? config.getString("name")
        : "UNNAMED_TRANSFORMER";
    String type = config.hasPath("type") && !config.getString("type").isBlank() ? config.getString("type") : null;

    Entry entry = type != null ? REGISTRY.get(type) : null;
    Spec composite = Spec.collect(entry != null ? entry.clazz() : AbstractTransformer.class);

    List<SpecException> exceptions = composite.validate(config, Transformer.TYPE, pipeline, name, index);

    if (type != null && entry == null) {
      exceptions.add(new SpecException(Transformer.TYPE, pipeline, name, "Unknown transformer type",
          index >= 0 ? MapUtils.ofNullable("index", index, "type", type) : MapUtils.ofNullable("type", type)));
    }

    return exceptions;
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
    List<SpecException> exceptions = validate(config, pipeline, index);

    if (!exceptions.isEmpty()) {
      throw exceptions.get(0);
    }

    return REGISTRY.get(config.getString("type")).factory().apply(config);
  }

  private record Entry(Class<? extends Transformer> clazz, Function<Config, Transformer> factory) {
  }
}
