package info.henrycaldwell.aggregator.core;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Record for referencing a media artifact.
 * 
 * This record defines a contract for carrying clip identity and essential
 * metadata.
 */
public record MediaRef(
    String id,
    Path file,
    URI uri,
    String title,
    String broadcaster,
    String language,
    List<String> tags
) {

  /**
   * Returns a new {@code MediaRef} with an updated file path.
   *
   * @param file A path representing the new file path.
   * @return A {@code MediaRef} representing the updated artifact.
   */
  public MediaRef withFile(Path file) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }

  /**
   * Returns a new {@code MediaRef} with an updated remote URI.
   *
   * @param uri A {@link URI} representing a remotely accessible location.
   * @return A {@code MediaRef} representing the updated artifact.
   */
  public MediaRef withUri(URI uri) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }

  /**
   * Returns a new {@code MediaRef} with updated tags.
   *
   * @param tags A list of strings representing the new tags.
   * @return A {@code MediaRef} representing the updated artifact.
   */
  public MediaRef withTags(List<String> tags) {
    return new MediaRef(id, file, uri, title, broadcaster, language, tags);
  }
}
