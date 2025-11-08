package info.henrycaldwell.aggregator.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Class for converting a video's frame rate.
 * 
 * This class re-samples the input to a target frames-per-second value.
 */
public class FpsTransformer implements Transformer {

  public static final Spec SPEC = Spec.builder()
      .requiredString("type", "ffmpegPath")
      .optionalNumber("targetFps")
      .build();

  private final String ffmpegPath;
  private final int targetFps;

  /**
   * Constructs an FpsTransformer.
   *
   * @param config A {@link Config} representing the transformer block.
   * @throws IllegalArgumentException if the target fps is not positive.
   */
  public FpsTransformer(Config config) {
    this.ffmpegPath = config.getString("ffmpegPath");

    int targetFps = config.getInt("targetFps");
    if (targetFps <= 0) {
      throw new IllegalArgumentException("Target fps must be positive (fps: " + targetFps + ")");
    }
    this.targetFps = targetFps;
  }

  /**
   * Applies a frame rate conversion.
   * 
   * @param media A {@code MediaRef} representing the current artifact.
   * @return A {@code MediaRef} representing the transformed artifact.
   * @throws RuntimeException         if the transform fails at any step.
   * @throws IllegalArgumentException if the input file is missing or not a
   *                                  regular file.
   */
  @Override
  public MediaRef apply(MediaRef media) {
    Path src = media.file();

    if (src == null || !Files.isRegularFile(src)) {
      throw new IllegalArgumentException("Input file missing or not a regular file (path: " + src + ")");
    }

    Path target = deriveOut(src, "-fps" + targetFps + ".mp4");
    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create parent directories (target: " + target + ")", e);
      }
    }

    if (Files.exists(target)) {
      throw new IllegalStateException("Target already exists (target: " + target + ")");
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
      throw new RuntimeException("Failed to start ffmpeg (path: " + ffmpegPath + ")", e);
    }

    boolean complete;
    try {
      complete = process.waitFor(2, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new RuntimeException("Interrupted while waiting for ffmpeg (clip: " + media.id() + ")", e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new RuntimeException("Timed out waiting for ffmpeg (clip: " + media.id() + ")");
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new RuntimeException("Exited ffmpeg with non-zero code " + code + " (clip: " + media.id() + ")");
    }

    if (!Files.exists(target)) {
      throw new RuntimeException("Output file missing (path: " + target + ")");
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new RuntimeException("Output file empty (path: " + target + ")");
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to stat output file (path: " + target + ")");
    }

    return media.withFile(target);
  }

  /**
   * Derives an output path by appending a suffix before the original file
   * extension.
   *
   * @param in     A path representing the input file.
   * @param suffix A string representing the suffix to append to the base
   *               filename.
   * @return A path representing the derived sibling file.
   */
  private static Path deriveOut(Path in, String suffix) {
    String name = in.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = (dot > 0) ? name.substring(0, dot) : name;

    return in.resolveSibling(base + suffix);
  }
}
