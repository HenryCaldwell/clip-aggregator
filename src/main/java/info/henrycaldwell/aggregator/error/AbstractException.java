package info.henrycaldwell.aggregator.error;

import java.util.Map;

/**
 * Base class for structured runtime exceptions.
 * 
 * This class formats error messages with an optional category, component, and
 * structured detail map for consistent logging and debugging.
 */
public abstract class AbstractException extends RuntimeException {

  /**
   * Constructs an abstract exception.
   * 
   * @param category  A string representing the high-level error category, or
   *                  {@code null}.
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   */
  protected AbstractException(
      String category,
      String component,
      String message) {
    super(format(category, component, message, null));
  }

  /**
   * Constructs an abstract exception.
   * 
   * @param category  A string representing the high-level error category, or
   *                  {@code null}.
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   */
  protected AbstractException(
      String category,
      String component,
      String message,
      Map<String, ?> details) {
    super(format(category, component, message, details));
  }

  /**
   * Constructs an abstract exception.
   * 
   * @param category  A string representing the high-level error category, or
   *                  {@code null}.
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   * @param cause     A {@link Throwable} representing the underlying cause, or
   *                  {@code null}.
   */
  protected AbstractException(
      String category,
      String component,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super(format(category, component, message, details), cause);
  }

  /**
   * Formats an error with an optional category, component, message, and detail
   * map.
   * 
   * @param category  A string representing the high-level error category, or
   *                  {@code null}.
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   * @return A string representing the formatted error message.
   */
  private static String format(
      String category,
      String component,
      String message,
      Map<String, ?> details) {
    StringBuilder sb = new StringBuilder();

    if (category != null || component != null) {
      sb.append('[');

      if (category != null && !category.isBlank()) {
        sb.append(category);
      }

      if (component != null && !component.isBlank()) {
        if (category != null && !category.isBlank()) {
          sb.append(':');
        }

        sb.append(component);
      }

      sb.append("] ");
    }

    if (message != null) {
      sb.append(message);
    }

    if (details != null && !details.isEmpty()) {
      sb.append(" (");
      boolean first = true;

      for (Map.Entry<String, ?> entry : details.entrySet()) {
        if (!first) {
          sb.append(", ");
        }

        first = false;
        sb.append(entry.getKey()).append('=').append(entry.getValue());
      }

      sb.append(')');
    }

    return sb.toString();
  }
}
