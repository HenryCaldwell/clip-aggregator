package info.henrycaldwell.aggregator.core;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Record for referencing a media artifact.
 * 
 * This record defines a contract for carrying media metadata used by components
 * that operate on media.
 */
public record MediaRef(
    String id,
    Path file,
    URI uri,
    String title,
    String broadcaster,
    String language,
    List<String> tags) {

  /**
   * Returns a new {@link MediaRef} with an updated file path.
   *
   * @param file A {@link Path} representing the updated file path.
   * @return A {@link MediaRef} representing the updated artifact.
   */
  public MediaRef withFile(Path file) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }

  /**
   * Returns a new {@link MediaRef} with an updated remote URI.
   *
   * @param uri A {@link URI} representing the updated remote URI.
   * @return A {@link MediaRef} representing the updated artifact.
   */
  public MediaRef withUri(URI uri) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }

  /**
   * Returns a new {@link MediaRef} with updated tags.
   *
   * @param tags A {@link List} of strings representing the updated tags.
   * @return A {@link MediaRef} representing the updated artifact.
   */
  public MediaRef withTags(List<String> tags) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }
}
