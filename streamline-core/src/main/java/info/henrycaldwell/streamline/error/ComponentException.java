package info.henrycaldwell.streamline.error;

import java.util.Map;

/**
 * Class for representing component runtime errors.
 * 
 * This class reports failures related to execution of a specific component such
 * as an observer, retriever, history, downloader, transformer, stager, or
 * publisher.
 */
public final class ComponentException extends AbstractException {

  /**
   * Constructs a ComponentException.
   *
   * @param type    A {@link ComponentType} representing the component type, or
   *                {@code null}.
   * @param parent  A string representing the parent component name, or
   *                {@code null}.
   * @param name    A string representing the component name, or {@code null}.
   * @param message A string representing the human-readable error message, or
   *                {@code null}.
   */
  public ComponentException(
      ComponentType type,
      String parent,
      String name,
      String message) {
    super("COMPONENT", type, parent, name, message);
  }

  /**
   * Constructs a ComponentException.
   *
   * @param type    A {@link ComponentType} representing the component type, or
   *                {@code null}.
   * @param parent  A string representing the parent component name, or
   *                {@code null}.
   * @param name    A string representing the component name, or {@code null}.
   * @param message A string representing the human-readable error message, or
   *                {@code null}.
   * @param details A {@link Map} representing detail values keyed by name, or
   *                {@code null}.
   */
  public ComponentException(
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details) {
    super("COMPONENT", type, parent, name, message, details);
  }

  /**
   * Constructs a ComponentException.
   *
   * @param type    A {@link ComponentType} representing the component type, or
   *                {@code null}.
   * @param parent  A string representing the parent component name, or
   *                {@code null}.
   * @param name    A string representing the component name, or {@code null}.
   * @param message A string representing the human-readable error message, or
   *                {@code null}.
   * @param details A {@link Map} representing detail values keyed by name, or
   *                {@code null}.
   * @param cause   A {@link Throwable} representing the underlying cause, or
   *                {@code null}.
   */
  public ComponentException(
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super("COMPONENT", type, parent, name, message, details, cause);
  }
}
