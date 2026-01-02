package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
public final class MusicTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ffmpegPath", "musicPath")
      .optionalString("mode")
      .optionalNumber("volume")
      .optionalBoolean("loop")
      .build();

  private final String ffmpegPath;
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

    this.ffmpegPath = config.getString("ffmpegPath");
    this.musicPath = config.getString("musicPath");

    String mode = config.hasPath("mode") ? config.getString("mode") : "mix";
    if (!"mix".equals(mode) && !"replace".equals(mode)) {
      throw new SpecException(name, "Invalid key value (expected position to be one of mix, replace)",
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
   * @throws ComponentException if transforming fails at any step.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new ComponentException(name, "Input file missing or not a regular file", Map.of("sourcePath", src));
    }

    Path target = deriveOut(src, "-music.mp4");
    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to create parent directories",
            Map.of("targetPath", target, "parentPath", parent), e);
      }
    }

    if (Files.exists(target)) {
      throw new ComponentException(name, "Target file already exists", Map.of("targetPath", target));
    }

    if (!Files.isRegularFile(Path.of(musicPath))) {
      throw new ComponentException(name, "Music file missing or not a regular file", Map.of("musicPath", musicPath));
    }

    String filterComplex = buildFilter();

    Process process;
    try {
      ProcessBuilder pb;
      if (loop) {
        pb = new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-i", src.toString(),
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
            "-i", src.toString(),
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
      }

      pb.redirectErrorStream(true);
      pb.redirectOutput(Redirect.DISCARD);
      process = pb.start();
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to start ffmpeg process",
          Map.of("ffmpegPath", ffmpegPath, "sourcePath", src, "targetPath", target), e);
    }

    boolean complete;
    try {
      complete = process.waitFor(2, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new ComponentException(name, "Interrupted while waiting for ffmpeg process", Map.of("clipId", media.id()),
          e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new ComponentException(name, "Timed out while waiting for ffmpeg process", Map.of("clipId", media.id()));
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new ComponentException(name, "ffmpeg process exited with non-zero code",
          Map.of("clipId", media.id(), "exitCode", code));
    }

    if (!Files.exists(target)) {
      throw new ComponentException(name, "Output file missing after transform", Map.of("targetPath", target));
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new ComponentException(name, "Output file empty after transform",
            Map.of("targetPath", target, "sizeBytes", size));
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to stat output file", Map.of("targetPath", target), e);
    }

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
