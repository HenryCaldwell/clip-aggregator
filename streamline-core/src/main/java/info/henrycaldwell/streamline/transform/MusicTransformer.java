package info.henrycaldwell.streamline.transform;

import java.nio.file.Files;
import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;
import info.henrycaldwell.streamline.util.PathUtils;

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
    this(config, null);
  }

  /**
   * Constructs a MusicTransformer with a custom process factory for testing.
   *
   * @param config  A {@link Config} representing the transformer configuration.
   * @param factory A {@link ProcessFactory} for creating the transformation
   *                subprocess,
   *                or {@code null} to use the default FFmpeg command.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  MusicTransformer(Config config, ProcessFactory factory) {
    super(config, SPEC, factory);

    this.musicPath = config.getString("musicPath");

    String mode = config.hasPath("mode") ? config.getString("mode") : "mix";
    if (!"mix".equals(mode) && !"replace".equals(mode)) {
      throw new SpecException(Transformer.TYPE, null, name,
          "Invalid key value (expected mode to be one of mix, replace)",
          MapUtils.ofNullable("key", "mode", "value", mode));
    }
    this.mode = mode;

    double volume = config.hasPath("volume") ? config.getNumber("volume").doubleValue() : 0.3;
    if (volume < 0.0) {
      throw new SpecException(Transformer.TYPE, null, name,
          "Invalid key value (expected volume to be greater than or equal to 0.0)",
          MapUtils.ofNullable("key", "volume", "value", volume));
    }
    this.volume = volume;

    this.loop = config.hasPath("loop") ? config.getBoolean("loop") : true;
  }

  /**
   * Applies a music transformation to the input media.
   * 
   * @param media A {@link MediaRef} representing the media to transform.
   * @param token A {@link CancellationToken} representing the cancellation
   *              signal.
   * @return A {@link MediaRef} representing the transformed media.
   */
  @Override
  public MediaRef apply(MediaRef media, CancellationToken token) {
    Path source = media.file();
    Path target = PathUtils.deriveOut(source, "-music.mp4");

    preflight(media, source, target);

    if (!Files.isRegularFile(Path.of(musicPath))) {
      throw new ComponentException(Transformer.TYPE, null, name, "Music file missing or not a regular file",
          MapUtils.ofNullable("musicPath", musicPath));
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

    runProcess(pb, media, source, target, token);
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
}
