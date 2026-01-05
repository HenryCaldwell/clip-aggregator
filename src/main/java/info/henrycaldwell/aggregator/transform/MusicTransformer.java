package info.henrycaldwell.aggregator.transform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;

/**
 * Class for layering music onto a video via the FFmpeg command-line utility.
 * 
 * This class invokes FFmpeg as a subprocess and either mixes or replaces the
 * original audio track with a music track.
 */
public final class MusicTransformer extends FFmpegTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("musicPath")
      .optionalString("mode")
      .optionalNumber("volume")
      .optionalBoolean("loop")
      .build();

  private final String musicPath;

  private final String mode;

  private final double volume;

  private final boolean loop;

  /**
   * Constructs a MusicTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public MusicTransformer(Config config) {
    super(config, SPEC);

    this.musicPath = config.getString("musicPath");

    String mode = config.hasPath("mode") ? config.getString("mode") : "mix";
    if (!"mix".equals(mode) && !"replace".equals(mode)) {
      throw new SpecException(name, "Invalid key value (expected mode to be one of mix, replace)",
          Map.of("key", "mode", "value", mode));
    }
    this.mode = mode;

    double volume = config.hasPath("volume") ? config.getNumber("volume").doubleValue() : 0.3;
    if (volume < 0.0) {
      throw new SpecException(name, "Invalid key value (expected volume to be greater than or equal to 0.0)",
          Map.of("key", "volume", "value", volume));
    }
    this.volume = volume;

    this.loop = config.hasPath("loop") ? config.getBoolean("loop") : true;
  }

  /**
   * Applies a music transformation to the input media.
   * 
   * @param media A {@link MediaRef} representing the media to transform.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path source = media.file();
    Path target = deriveOut(source, "-music.mp4");

    preflight(media, source, target);

    if (!Files.isRegularFile(Path.of(musicPath))) {
      throw new ComponentException(name, "Music file missing or not a regular file", Map.of("musicPath", musicPath));
    }

    String filterComplex = buildFilter();

    ProcessBuilder pb;
    if (loop) {
      pb = new ProcessBuilder(
          ffmpegPath,
          "-y",
          "-i", source.toString(),
          "-stream_loop", "-1",
          "-i", musicPath,
          "-filter_complex", filterComplex,
          "-map", "0:v:0",
          "-map", "[aout]",
          "-c:v", "libx264",
          "-pix_fmt", "yuv420p",
          "-c:a", "aac",
          "-b:a", "128k",
          "-ar", "48000",
          "-shortest",
          target.toString());
    } else {
      pb = new ProcessBuilder(
          ffmpegPath,
          "-y",
          "-i", source.toString(),
          "-i", musicPath,
          "-filter_complex", filterComplex,
          "-map", "0:v:0",
          "-map", "[aout]",
          "-c:v", "libx264",
          "-pix_fmt", "yuv420p",
          "-c:a", "aac",
          "-b:a", "128k",
          "-ar", "48000",
          "-shortest",
          target.toString());
    }

    runProcess(pb, media, source, target);
    postflight(media, source, target);

    return media.withFile(target);
  }

  /**
   * Builds the FFmpeg filter expression.
   *
   * @return A string representing the FFmpeg filter expression.
   */
  private String buildFilter() {
    StringBuilder sb = new StringBuilder();

    sb.append("[1:a]");
    sb.append("volume=").append(String.format(java.util.Locale.ROOT, "%.6f", volume)).append(",");
    sb.append("aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[music];");

    if ("replace".equals(mode)) {
      sb.append("[music]anull[aout]");

      return sb.toString();
    }

    sb.append("[0:a]aformat=sample_fmts=fltp:sample_rates=48000:channel_layouts=stereo[orig];")
        .append("[orig][music]amix=inputs=2:duration=shortest:dropout_transition=2[aout]");

    return sb.toString();
  }

  /**
   * Derives an output path by appending a suffix before the original file
   * extension.
   *
   * @param in     A {@link Path} representing the input file.
   * @param suffix A string representing the suffix to append to the base
   *               filename.
   * @return A {@link Path} representing the derived sibling file.
   */
  private static Path deriveOut(Path in, String suffix) {
    String name = in.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = (dot > 0) ? name.substring(0, dot) : name;

    return in.resolveSibling(base + suffix);
  }
}
