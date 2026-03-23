package info.henrycaldwell.aggregator.util;

import java.nio.file.Path;

/**
 * Utility class for path operations.
 *
 * This class provides helpers for deriving file paths.
 */
public final class PathUtils {

  private PathUtils() {
  }

  /**
   * Derives an output path by appending a suffix before the original file
   * extension.
   *
   * @param in     A {@link Path} representing the input file.
   * @param suffix A string representing the suffix to append to the base
   *               filename.
   * @return A {@link Path} representing the derived sibling file.
   */
  public static Path deriveOut(Path in, String suffix) {
    String name = in.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String base = (dot > 0) ? name.substring(0, dot) : name;

    return in.resolveSibling(base + suffix);
  }
}
