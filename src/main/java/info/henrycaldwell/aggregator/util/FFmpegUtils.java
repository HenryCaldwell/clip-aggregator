package info.henrycaldwell.aggregator.util;

/**
 * Utility class for FFmpeg filter expression operations.
 * 
 * This class provides helpers for constructing and manipulating FFmpeg filter
 * arguments.
 */
public final class FFmpegUtils {

  private FFmpegUtils() {
  }

  /**
   * Adds an integer pixel offset to an FFmpeg position expression.
   *
   * @param expr   A string representing the base FFmpeg expression.
   * @param offset An integer representing the pixel offset.
   * @return A string representing the updated FFmpeg expression.
   */
  public static String addOffset(String expr, int offset) {
    if (offset == 0)
      return expr;
    return expr + (offset > 0 ? "+" : "") + offset;
  }

  /**
   * Normalizes a path for safe use in an FFmpeg filter argument.
   *
   * @param path A string representing the path to normalize.
   * @return A string representing the normalized path.
   */
  public static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return path;
    }

    return path.replace("\\", "/").replace(":", "\\:");
  }

  /**
   * Escapes a string for safe use in an FFmpeg filter argument.
   *
   * @param text A string representing the text to escape.
   * @return A string representing the escaped text.
   */
  public static String escapeText(String text) {
    if (text == null) {
      return null;
    }

    return text.replace("'", "\\'");
  }
}
