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
 * Class for converting a video's frame rate via the FFmpeg command-line
 * utility.
 * 
 * This class invokes FFmpeg as a subprocess and re-samples the clip to a target
 * frames-per-second value.
 */
public final class FpsTransformer extends AbstractTransformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ffmpegPath")
      .optionalNumber("targetFps")
      .build();

  private final String ffmpegPath;

  private final int targetFps;

  /**
   * Constructs an FpsTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  public FpsTransformer(Config config) {
    super(config, SPEC);

    this.ffmpegPath = config.getString("ffmpegPath");

    int targetFps = config.hasPath("targetFps") ? config.getNumber("targetFps").intValue() : 30;
    if (targetFps <= 0) {
      throw new SpecException(name, "Invalid key value (expected targetFps to be greater than 0)",
          Map.of("key", "targetFps", "value", targetFps));
    }

    this.targetFps = targetFps;
  }

  /**
   * Applies a frame rate transformation to the input media.
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

    Path target = deriveOut(src, "-temp.mp4");
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

    Process process;
    try {
      ProcessBuilder pb = new ProcessBuilder(
          ffmpegPath,
          "-y",
          "-i", src.toString(),
          "-r", Integer.toString(targetFps),
          "-c:v", "libx264",
          "-pix_fmt", "yuv420p",
          "-c:a", "aac",
          "-b:a", "128k",
          "-ar", "48000",
          target.toString());
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
