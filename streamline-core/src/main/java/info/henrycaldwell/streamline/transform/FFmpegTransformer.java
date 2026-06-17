package info.henrycaldwell.streamline.transform;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.Cancellable;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

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

  private final ProcessFactory factory;

  /**
   * Constructs an FFmpegTransformer.
   *
   * @param config A {@link Config} representing the transformer configuration.
   * @param spec   A {@link Spec} representing the subclass-specific spec.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  protected FFmpegTransformer(Config config, Spec spec) {
    this(config, spec, null);
  }

  /**
   * Constructs an FFmpegTransformer with a custom process factory for testing.
   *
   * @param config  A {@link Config} representing the transformer configuration.
   * @param spec    A {@link Spec} representing the subclass-specific spec.
   * @param factory A {@link ProcessFactory} for creating the transformation
   *                subprocess,
   *                or {@code null} to use the default FFmpeg command.
   * @throws SpecException if the configuration violates the transformer spec.
   */
  protected FFmpegTransformer(Config config, Spec spec, ProcessFactory factory) {
    super(config, Spec.union(FFMPEG_SPEC, spec));

    this.ffmpegPath = config.getString("ffmpegPath");

    long timeout = config.hasPath("timeout") ? config.getNumber("timeout").longValue() : 180L;
    if (timeout <= 0) {
      throw new SpecException(name, "Invalid key value (expected timeout to be greater than 0)",
          MapUtils.ofNullable("key", "timeout", "value", timeout));
    }
    this.timeout = timeout;

    this.factory = factory != null ? factory : ProcessBuilder::start;
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
          MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source));
    }

    if (target == null) {
      throw new ComponentException(name, "Target path is null",
          MapUtils.ofNullable("clipId", media.clip().id(), "targetPath", target));
    }

    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new ComponentException(name, "Failed to create parent directories",
            MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target, "parentPath",
                parent),
            e);
      }
    }

    if (Files.exists(target)) {
      throw new ComponentException(name, "Target file already exists",
          MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target));
    }
  }

  /**
   * Runs an FFmpeg subprocess and validates its exit status.
   *
   * @param pb     A {@link ProcessBuilder} representing the FFmpeg subprocess.
   * @param media  A {@link MediaRef} representing the media to transform.
   * @param source A {@link Path} representing the source file.
   * @param target A {@link Path} representing the target file.
   * @param token  A {@link CancellationToken} representing the cancellation
   *               signal.
   * @throws ComponentException if the FFmpeg process fails at any step.
   */
  protected final void runProcess(ProcessBuilder pb, MediaRef media, Path source, Path target,
      CancellationToken token) {
    Process process;
    try {
      pb.redirectErrorStream(true);
      pb.redirectOutput(Redirect.DISCARD);
      process = factory.start(pb);
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to start ffmpeg process", MapUtils.ofNullable("ffmpegPath", ffmpegPath,
          "clipId", media.clip().id(), "sourcePath", source, "targetPath", target), e);
    }

    Cancellable kill = () -> process.destroy();
    token.register(kill);

    try {
      boolean complete;
      try {
        complete = process.waitFor(timeout, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        process.destroyForcibly();
        throw new ComponentException(name, "Interrupted while waiting for ffmpeg process", MapUtils
            .ofNullable("ffmpegPath", ffmpegPath, "clipId", media.clip().id(), "sourcePath", source, "targetPath",
                target),
            e);
      }

      if (!complete) {
        process.destroyForcibly();
        throw new ComponentException(name, "Timed out while waiting for ffmpeg process",
            MapUtils.ofNullable("ffmpegPath",
                ffmpegPath, "clipId", media.clip().id(), "sourcePath", source, "targetPath", target, "timeout",
                timeout));
      }

      int code = process.exitValue();
      if (code != 0) {
        throw new ComponentException(name, "ffmpeg process exited with non-zero code", MapUtils.ofNullable("ffmpegPath",
            ffmpegPath, "clipId", media.clip().id(), "sourcePath", source, "targetPath", target, "exitCode", code));
      }
    } finally {
      token.unregister(kill);
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
          MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target));
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new ComponentException(name, "Output file empty after transform",
            MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target, "sizeBytes",
                size));
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to stat output file",
          MapUtils.ofNullable("clipId", media.clip().id(), "sourcePath", source, "targetPath", target), e);
    }
  }
}
