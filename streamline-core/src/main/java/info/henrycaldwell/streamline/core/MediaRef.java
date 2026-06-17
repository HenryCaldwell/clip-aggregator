package info.henrycaldwell.streamline.core;

import java.net.URI;
import java.nio.file.Path;

/**
 * Record for referencing a media artifact.
 * 
 * This record defines a contract for carrying media metadata used by components
 * that operate on media.
 */
public record MediaRef(
    ClipRef clip,
    Path file,
    URI uri) {

  /**
   * Returns a new {@link MediaRef} with an updated file path.
   *
   * @param file A {@link Path} representing the updated file path.
   * @return A {@link MediaRef} representing the updated artifact.
   */
  public MediaRef withFile(Path file) {
    return new MediaRef(clip, file, uri);
  }

  /**
   * Returns a new {@link MediaRef} with an updated remote URI.
   *
   * @param uri A {@link URI} representing the updated remote URI.
   * @return A {@link MediaRef} representing the updated artifact.
   */
  public MediaRef withUri(URI uri) {
    return new MediaRef(clip, file, uri);
  }
}
