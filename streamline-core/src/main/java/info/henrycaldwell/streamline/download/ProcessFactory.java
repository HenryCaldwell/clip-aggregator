package info.henrycaldwell.streamline.download;

import java.nio.file.Path;

import info.henrycaldwell.streamline.core.ClipRef;

/**
 * Functional interface for creating a downloader subprocess.
 *
 * This interface defines a contract for constructing the subprocess used by
 * downloaders, allowing the real command to be substituted in tests.
 */
@FunctionalInterface
interface ProcessFactory {

  /**
   * Creates a download subprocess for the given clip and target path.
   *
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the media destination.
   * @return A {@link ProcessBuilder} configured to produce the target file.
   */
  ProcessBuilder create(ClipRef clip, Path target);
}
