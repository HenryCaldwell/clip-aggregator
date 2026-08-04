package info.henrycaldwell.streamline.error;

import java.util.Map;

/**
 * Class for representing configuration specification errors.
 *
 * This class reports failures related to configuration structure, required
 * fields, and type mismatches detected during validation.
 */
public final class SpecException extends AbstractException {

  /**
   * Constructs a SpecException.
   *
   * @param type    A {@link ComponentType} representing the component type, or
   *                {@code null}.
   * @param parent  A string representing the parent component name, or
   *                {@code null}.
   * @param name    A string representing the component name, or {@code null}.
   * @param message A string representing the human-readable error message, or
   *                {@code null}.
   */
  public SpecException(
      ComponentType type,
      String parent,
      String name,
      String message) {
    super("SPEC", type, parent, name, message);
  }

  /**
   * Constructs a SpecException.
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
  public SpecException(
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details) {
    super("SPEC", type, parent, name, message, details);
  }

  /**
   * Constructs a SpecException.
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
  public SpecException(
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super("SPEC", type, parent, name, message, details, cause);
  }
}
