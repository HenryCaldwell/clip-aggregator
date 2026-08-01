package info.henrycaldwell.streamline.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.NumberConstraint;
import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.Cancellable;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for downloading clips via the yt-dlp command-line extractor.
 *
 * This class invokes yt-dlp as a subprocess and writes the resulting media
 * file.
 */
public final class YtDlpDownloader extends AbstractDownloader {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ytDlpPath")
      .optionalNumber(NumberConstraint.greaterThan(0), "timeout")
      .build();

  private final String ytDlpPath;

  private final long timeout;

  private final ProcessFactory factory;

  /**
   * Constructs a YtDlpDownloader.
   *
   * @param config A {@link Config} representing the downloader configuration.
   */
  public YtDlpDownloader(Config config) {
    this(config, null);
  }

  /**
   * Constructs a YtDlpDownloader with a custom process factory for testing.
   *
   * @param config  A {@link Config} representing the downloader configuration.
   * @param factory A {@link ProcessFactory} for creating the download subprocess,
   *                or {@code null} to use the default yt-dlp command.
   */
  YtDlpDownloader(Config config, ProcessFactory factory) {
    super(config, SPEC);

    this.ytDlpPath = config.getString("ytDlpPath");

    this.timeout = config.hasPath("timeout") ? config.getNumber("timeout").longValue() : 180L;

    this.factory = factory != null ? factory : this::defaultCreate;
  }

  /**
   * Downloads the input clip to the specified path.
   *
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the media destination.
   * @param token  A {@link CancellationToken} representing the cancellation
   *               signal.
   * @return A {@link MediaRef} representing the downloaded media.
   * @throws ComponentException if downloading fails at any step.
   */
  @Override
  public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
    Path parent = target.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (IOException e) {
        throw new ComponentException(Downloader.TYPE, null, name, "Failed to create parent directories",
            MapUtils.ofNullable("targetPath", target, "parentPath", parent), e);
      }
    }

    if (Files.exists(target)) {
      throw new ComponentException(Downloader.TYPE, null, name, "Target file already exists",
          MapUtils.ofNullable("targetPath", target));
    }

    Path temp = target.resolveSibling(target.getFileName().toString() + ".part");
    try {
      Files.deleteIfExists(temp);
    } catch (IOException ignored) {
    }

    Process process;
    try {
      ProcessBuilder pb = factory.create(clip, target);
      pb.redirectErrorStream(true);
      process = pb.start();
    } catch (IOException e) {
      throw new ComponentException(Downloader.TYPE, null, name, "Failed to start yt-dlp process",
          MapUtils.ofNullable("ytDlpPath", ytDlpPath, "clipId", clip.id(), "targetPath", target), e);
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
        throw new ComponentException(Downloader.TYPE, null, name, "Interrupted while waiting for yt-dlp process",
            MapUtils.ofNullable("clipId", clip.id()), e);
      }

      if (!complete) {
        process.destroyForcibly();
        throw new ComponentException(Downloader.TYPE, null, name, "Timed out while waiting for yt-dlp process",
            MapUtils.ofNullable("clipId", clip.id(), "timeout", timeout));
      }

      int code = process.exitValue();
      if (code != 0) {
        throw new ComponentException(Downloader.TYPE, null, name, "yt-dlp process exited with non-zero code",
            MapUtils.ofNullable("clipId", clip.id(), "exitCode", code));
      }
    } finally {
      token.unregister(kill);
    }

    if (!Files.exists(target)) {
      throw new ComponentException(Downloader.TYPE, null, name, "Output file missing after download",
          MapUtils.ofNullable("targetPath", target));
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new ComponentException(Downloader.TYPE, null, name, "Output file empty after download",
            MapUtils.ofNullable("targetPath", target, "sizeBytes", size));
      }
    } catch (IOException e) {
      throw new ComponentException(Downloader.TYPE, null, name, "Failed to stat output file",
          MapUtils.ofNullable("targetPath", target), e);
    }

    return new MediaRef(clip, target, null);
  }

  /**
   * Creates the download subprocess using the default yt-dlp command.
   *
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the media destination.
   * @return A {@link ProcessBuilder} configured to invoke yt-dlp.
   */
  private ProcessBuilder defaultCreate(ClipRef clip, Path target) {
    return new ProcessBuilder(
        ytDlpPath,
        clip.url(),
        "--restrict-filenames",
        "--windows-filenames",
        "-o", target.toString());
  }
}
