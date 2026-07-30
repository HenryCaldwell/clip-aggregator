package info.henrycaldwell.streamline.download;

import java.nio.file.Path;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.error.ComponentType;

/**
 * Interface for downloading clips.
 *
 * This interface defines a contract for producing local media from input clips.
 */
public interface Downloader {

  ComponentType TYPE = ComponentType.DOWNLOADER;

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
   * @param token  A {@link CancellationToken} representing the cancellation
   *               signal.
   * @return A {@link MediaRef} representing the downloaded media.
   */
  MediaRef download(ClipRef clip, Path target, CancellationToken token);
}
