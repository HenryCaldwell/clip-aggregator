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
 * Base class for transformers that invoke FFmpeg.
 *
 * This class parses shared FFmpeg configuration and provides helper methods for
 * FFmpeg processes.
 */
public abstract class FFmpegTransformer extends AbstractTransformer {

  protected static final Spec FFMPEG_SPEC = Spec.builder()
      .requiredString("ffmpegPath")
      .optionalNumber("timeout")
      .build();

  protected final String ffmpegPath;
  protected final long timeout;

  /**
   * Constructs an FFmpegTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  protected FFmpegTransformer(Config config, Spec spec) {
    super(config, Spec.union(FFMPEG_SPEC, spec));

    this.ffmpegPath = config.getString("ffmpegPath");

    long timeout = config.hasPath("timeout") ? config.getNumber("timeout").longValue() : 180L;
    if (timeout <= 0) {
      throw new SpecException(name, "Invalid key value (expected timeout to be greater than 0)",
          Map.of("key", "timeout", "value", timeout));
    }
    this.timeout = timeout;
  }

  /**
   * Validates the input file and target path.
   *
   * @param media  A {@link MediaRef} representing the media to transform.
   * @param source A {@link Path} representing the source file.
   * @param target A {@link Path} representing the target file.
   * @throws ComponentException if validation fails at any step.
   */
  protected final void preflight(MediaRef media, Path source, Path target) {
    if (source == null || !Files.isRegularFile(source)) {
      throw new ComponentException(name, "Input file missing or not a regular file",
          Map.of("clipId", media.id(), "sourcePath", source));
    }

    if (target == null) {
      throw new ComponentException(name, "Target path is null", Map.of("clipId", media.id(), "targetPath", target));
    }

    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to create parent directories",
            Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target, "parentPath", parent), e);
      }
    }

    if (Files.exists(target)) {
      throw new ComponentException(name, "Target file already exists",
          Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target));
    }
  }

  /**
   * Runs an FFmpeg subprocess and validates its exit status.
   *
   * @param pb     A {@link ProcessBuilder} representing the FFmpeg subprocess.
   * @param media  A {@link MediaRef} representing the media to transform.
   * @param source A {@link Path} representing the source file.
   * @param target A {@link Path} representing the target file.
   * @throws ComponentException if the FFmpeg process fails at any step.
   */
  protected final void runProcess(ProcessBuilder pb, MediaRef media, Path source, Path target) {
    Process process;
    try {
      pb.redirectErrorStream(true);
      pb.redirectOutput(Redirect.DISCARD);
      process = pb.start();
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to start ffmpeg process",
          Map.of("ffmpegPath", ffmpegPath, "clipId", media.id(), "sourcePath", source, "targetPath", target), e);
    }

    boolean complete;
    try {
      complete = process.waitFor(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new ComponentException(name, "Interrupted while waiting for ffmpeg process",
          Map.of("ffmpegPath", ffmpegPath, "clipId", media.id(), "sourcePath", source, "targetPath", target), e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new ComponentException(name, "Timed out while waiting for ffmpeg process",
          Map.of("ffmpegPath", ffmpegPath, "clipId", media.id(), "sourcePath", source, "targetPath", target, "timeout",
              timeout));
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new ComponentException(name, "ffmpeg process exited with non-zero code",
          Map.of("ffmpegPath", ffmpegPath, "clipId", media.id(), "sourcePath", source, "targetPath", target, "exitCode",
              code));
    }
  }

  /**
   * Validates the output file.
   *
   * @param media  A {@link MediaRef} representing the media to transform.
   * @param source A {@link Path} representing the source file.
   * @param target A {@link Path} representing the target file.
   * @throws ComponentException if validation fails at any step.
   */
  protected final void postflight(MediaRef media, Path source, Path target) {
    if (!Files.exists(target)) {
      throw new ComponentException(name, "Output file missing after transform",
          Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target));
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new ComponentException(name, "Output file empty after transform",
            Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target, "sizeBytes", size));
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to stat output file",
          Map.of("clipId", media.id(), "sourcePath", source, "targetPath", target), e);
    }
  }
}
