package info.henrycaldwell.aggregator.download;

import java.nio.file.Path;

import info.henrycaldwell.aggregator.core.ClipRef;
import info.henrycaldwell.aggregator.core.MediaRef;

/**
 * Interface for downloading clips.
 * 
 * This interface defines a contract for producing local media from input clips.
 */
public interface Downloader {

  /**
   * Returns the configured downloader name.
   *
   * @return A string representing the downloader name.
   */
  String getName();

  /**
   * Downloads the input clip to the specified path.
   * 
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the media destination.
   * @return A {@link MediaRef} representing the downloaded media.
   */
  MediaRef download(ClipRef clip, Path target);
}
