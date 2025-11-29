package info.henrycaldwell.aggregator.error;

import java.util.Map;

/**
 * Class for representing component runtime errors.
 * 
 * This class identifies failures related to execution of a specific component
 * such as a retriever, history, downloader, transformer, stager, or publisher.
 */
public class ComponentException extends AbstractException {

  /**
   * Constructs a component exception with a message.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   */
  public ComponentException(
      String component,
      String message) {
    super("COMPONENT", component, message);
  }

  /**
   * Constructs a component exception with a message and detail map.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   */
  public ComponentException(
      String component,
      String message,
      Map<String, ?> details) {
    super("COMPONENT", component, message, details);
  }

  /**
   * Constructs a component exception with a message, detail map, and cause.
   * 
   * @param component A string representing the component name, or {@code null}.
   * @param message   A string representing the human-readable error message, or
   *                  {@code null}.
   * @param details   A {@link Map} representing detail values keyed by name, or
   *                  {@code null}.
   * @param cause     A {@link Throwable} representing the underlying cause, or
   *                  {@code null}.
   */
  public ComponentException(
      String component,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super("COMPONENT", component, message, details, cause);
  }
}
