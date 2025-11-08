package info.henrycaldwell.aggregator.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.core.ClipRef;
import info.henrycaldwell.aggregator.core.DownloadRef;

/**
 * Class for downloading clips via the yt-dlp command-line extractor.
 * 
 * This class invokes yt-dlp as a subprocess and writes the resulting media
 * file.
 */
public class YtDlpDownloader implements Downloader {

  public static final Spec SPEC = Spec.builder()
      .requiredString("name", "type", "ytDlpPath")
      .build();

  private final String name;
  private final String ytDlpPath;

  /**
   * Constructs a YtDlpDownloader.
   *
   * @param config A {@link Config} representing the transformer block.
   */
  public YtDlpDownloader(Config config) {
    this.name = config.getString("name");
    this.ytDlpPath = config.getString("ytDlpPath");
  }

  /**
   * Downloads a single clip to the specified path.
   *
   * @param clip   A record referencing the clip to download.
   * @param target A path representing the destination media file.
   * @return A {@link DownloadRef} representing the downloaded file.
   * @throws RuntimeException if the download fails at any step.
   */
  @Override
  public DownloadRef download(ClipRef clip, Path target) {
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
      throw new RuntimeException("Failed to start yt-dlp (path: " + ytDlpPath + ")", e);
    }

    boolean complete;
    try {
      complete = process.waitFor(10, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      throw new RuntimeException("Interrupted while waiting for yt-dlp (clip: " + clip.id() + ")", e);
    }

    if (!complete) {
      process.destroyForcibly();
      throw new RuntimeException("Timed out waiting for yt-dlp (clip: " + clip.id() + ")");
    }

    int code = process.exitValue();
    if (code != 0) {
      throw new RuntimeException("Exited yt-dlp with non-zero code " + code + " (clip: " + clip.id() + ")");
    }

    if (!Files.exists(target)) {
      throw new RuntimeException("Output file missing (path: " + temp + ")");
    }

    try {
      long size = Files.size(target);

      if (size <= 0) {
        throw new RuntimeException("Output file empty (path: " + temp + ")");
      }

      return new DownloadRef(clip.id(), target, size);
    } catch (IOException e) {
      throw new RuntimeException("Failed to stat output file (path: " + target + ")");
    }
  }
}
