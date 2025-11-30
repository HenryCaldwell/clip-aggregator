package info.henrycaldwell.aggregator.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.ClipRef;
import info.henrycaldwell.aggregator.core.MediaRef;
import info.henrycaldwell.aggregator.error.ComponentException;

/**
 * Class for downloading clips via the yt-dlp command-line extractor.
 * 
 * This class invokes yt-dlp as a subprocess and writes the resulting media
 * file.
 */
public final class YtDlpDownloader extends AbstractDownloader {

  public static final Spec SPEC = Spec.builder()
      .requiredString("ytDlpPath")
      .build();

  private final String ytDlpPath;

  /**
   * Constructs a YtDlpDownloader.
   *
   * @param config A {@link Config} representing the downloader block.
   */
  public YtDlpDownloader(Config config) {
    super(config, SPEC);

    this.ytDlpPath = config.getString("ytDlpPath");
  }

  /**
   * Downloads a single clip to the specified path.
   *
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the destination media file.
   * @return @return A {@link MediaRef} representing the downloaded artifact.
   * @throws RuntimeException if the download fails at any step.
   */
  @Override
  public MediaRef download(ClipRef clip, Path target) {
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

    Path temp = target.resolveSibling(target.getFileName().toString() + ".part");
    try {
      Files.deleteIfExists(temp);
    } catch (IOException ignored) {
    }

    Process process;
    try {
      ProcessBuilder pb = new ProcessBuilder(
          ytDlpPath,
          clip.url(),
          "--restrict-filenames",
          "--windows-filenames",
          "-o", target.toString());
      pb.redirectErrorStream(true);
      process = pb.start();
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to start yt-dlp process",
          Map.of("ytDlpPath", ytDlpPath, "clipId", clip.id(), "targetPath", target), e);
    }

    boolean complete;
    try {
      complete = process.waitFor(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new ComponentException(name, "Interrupted while waiting for yt-dlp process", Map.of("clipId", clip.id()),
          e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new ComponentException(name, "Timed out while waiting for yt-dlp process", Map.of("clipId", clip.id()));
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new ComponentException(name, "yt-dlp process exited with non-zero code",
          Map.of("clipId", clip.id(), "exitCode", code));
    }

    if (!Files.exists(target)) {
      throw new ComponentException(name, "Output file missing after download", Map.of("targetPath", target));
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new ComponentException(name, "Output file empty after download",
            Map.of("targetPath", target, "sizeBytes", size));
      }
    } catch (IOException e) {
      throw new ComponentException(name, "Failed to stat output file", Map.of("targetPath", target), e);
    }

    return new MediaRef(
        clip.id(),
        target,
        null,
        clip.title(),
        clip.broadcaster(),
        clip.language(),
        List.of());
  }
}
