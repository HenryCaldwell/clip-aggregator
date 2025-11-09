package info.henrycaldwell.aggregator.download;

import java.nio.file.Path;

import info.henrycaldwell.aggregator.core.ClipRef;
import info.henrycaldwell.aggregator.core.DownloadRef;

/**
 * Interface for downloading clips to a target file path.
 * 
 * This interface defines a contract for retrieving a media file for a clip.
 */
public interface Downloader {

  /**
   * Returns the configured downloader name.
   *
   * @return A string representing the downloader name.
   */
  String getName();

  /**
   * Downloads a single clip to the specified path.
   * 
   * @param clip A record referencing the clip to download.
   * @param path A path representing the destination media file.
   * @return A {@link DownloadRef} representing the downloaded file.
   */
  DownloadRef download(ClipRef clip, Path path);
}
