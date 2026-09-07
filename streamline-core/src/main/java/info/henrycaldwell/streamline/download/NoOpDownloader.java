package info.henrycaldwell.streamline.download;

import java.nio.file.Path;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.core.MediaRef;

/**
 * Class for downloading clips by performing no action.
 *
 * This class accepts a clip without downloading any file.
 */
public final class NoOpDownloader extends AbstractDownloader {

  public static final Spec SPEC = Spec.builder().build();

  /**
   * Constructs a NoOpDownloader.
   *
   * @param config A {@link Config} representing the downloader configuration.
   */
  public NoOpDownloader(Config config) {
    super(config);
  }

  /**
   * Downloads the input clip by performing no action.
   *
   * @param clip   A {@link ClipRef} representing the clip to download.
   * @param target A {@link Path} representing the media destination.
   * @param token  A {@link CancellationToken} representing the cancellation
   *               signal.
   * @return A {@link MediaRef} representing the downloaded media.
   */
  @Override
  public MediaRef download(ClipRef clip, Path target, CancellationToken token) {
    return new MediaRef(clip, target, null);
  }
}
