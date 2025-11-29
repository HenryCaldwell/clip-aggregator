package info.henrycaldwell.aggregator.error;

import java.util.Map;

/**
 * Class for representing configuration specification errors.
 * 
 * This class identifies failures related to configuration shapes, required
 * fields, and type mismatches detected during specification validation.
 */
public class SpecException extends AbstractException {

  /**
   * Constructs a specification exception with a message.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   */
  public SpecException(
      String component,
      String message) {
    super("SPEC", component, message);
  }

  /**
   * Constructs a specification exception with a message and detail map.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   */
  public SpecException(
      String component,
      String message,
      Map<String, ?> details) {
    super("SPEC", component, message, details);
  }

  /**
   * Constructs a specification exception with a message, detail map, and cause.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   * @param cause     A {@link Throwable} representing the underlying cause, or
   *                  {@code null}.
   */
  public SpecException(
      String component,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super("SPEC", component, message, details, cause);
  }
}
