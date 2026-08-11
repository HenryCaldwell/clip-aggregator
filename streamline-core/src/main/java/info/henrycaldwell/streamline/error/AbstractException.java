package info.henrycaldwell.streamline.error;

import java.util.Map;

/**
 * Base class for structured runtime exceptions.
 *
 * This class formats error messages with an optional category, type, parent,
 * name, and structured detail map for consistent logging and debugging.
 */
public abstract class AbstractException extends RuntimeException {

  private final String category;
  private final ComponentType type;
  private final String parent;
  private final String name;
  private final Map<String, ?> details;

  /**
   * Constructs an abstract exception.
   *
   * @param category A string representing the high-level error category, or
   *                 {@code null}.
   * @param type     A {@link ComponentType} representing the component type, or
   *                 {@code null}.
   * @param parent   A string representing the parent component name, or
   *                 {@code null}.
   * @param name     A string representing the component name, or {@code null}.
   * @param message  A string representing the human-readable error message, or
   *                 {@code null}.
   */
  protected AbstractException(
      String category,
      ComponentType type,
      String parent,
      String name,
      String message) {
    this(category, type, parent, name, message, null, null);
  }

  /**
   * Constructs an abstract exception.
   *
   * @param category A string representing the high-level error category, or
   *                 {@code null}.
   * @param type     A {@link ComponentType} representing the component type, or
   *                 {@code null}.
   * @param parent   A string representing the parent component name, or
   *                 {@code null}.
   * @param name     A string representing the component name, or {@code null}.
   * @param message  A string representing the human-readable error message, or
   *                 {@code null}.
   * @param details  A {@link Map} representing detail values keyed by name, or
   *                 {@code null}.
   */
  protected AbstractException(
      String category,
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details) {
    this(category, type, parent, name, message, details, null);
  }

  /**
   * Constructs an abstract exception.
   *
   * @param category A string representing the high-level error category, or
   *                 {@code null}.
   * @param type     A {@link ComponentType} representing the component type, or
   *                 {@code null}.
   * @param parent   A string representing the parent component name, or
   *                 {@code null}.
   * @param name     A string representing the component name, or {@code null}.
   * @param message  A string representing the human-readable error message, or
   *                 {@code null}.
   * @param details  A {@link Map} representing detail values keyed by name, or
   *                 {@code null}.
   * @param cause    A {@link Throwable} representing the underlying cause, or
   *                 {@code null}.
   */
  protected AbstractException(
      String category,
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details,
      Throwable cause) {
    super(format(category, type, parent, name, message, details), cause);
    this.category = category;
    this.type = type;
    this.parent = parent;
    this.name = name;
    this.details = details;
  }

  /**
   * Returns the error category.
   *
   * @return A string representing the error category, or {@code null}.
   */
  public String getCategory() {
    return category;
  }

  /**
   * Returns the component type.
   *
   * @return A {@link ComponentType} representing the component type, or
   *         {@code null}.
   */
  public ComponentType getType() {
    return type;
  }

  /**
   * Returns the parent component name.
   *
   * @return A string representing the parent component name, or {@code null}.
   */
  public String getParent() {
    return parent;
  }

  /**
   * Returns the component name.
   *
   * @return A string representing the component name, or {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the structured detail map.
   *
   * @return A {@link Map} representing detail values keyed by name, or
   *         {@code null}.
   */
  public Map<String, ?> getDetails() {
    return details;
  }

  /**
   * Formats an error with an optional category, type, parent, name, message,
   * and detail map.
   *
   * @param category A string representing the high-level error category, or
   *                 {@code null}.
   * @param type     A {@link ComponentType} representing the component type, or
   *                 {@code null}.
   * @param parent   A string representing the parent component name, or
   *                 {@code null}.
   * @param name     A string representing the component name, or {@code null}.
   * @param message  A string representing the human-readable error message, or
   *                 {@code null}.
   * @param details  A {@link Map} representing detail values keyed by name, or
   *                 {@code null}.
   * @return A string representing the formatted error message.
   */
  private static String format(
      String category,
      ComponentType type,
      String parent,
      String name,
      String message,
      Map<String, ?> details) {
    StringBuilder sb = new StringBuilder();

    boolean hasCategory = category != null && !category.isBlank();
    boolean hasType = type != null;
    boolean hasParent = parent != null && !parent.isBlank();
    boolean hasName = name != null && !name.isBlank();

    if (hasCategory || hasType || hasParent || hasName) {
      sb.append('[');

      boolean needsSeparator = false;

      if (hasCategory) {
        sb.append(category);
        needsSeparator = true;
      }

      if (hasType) {
        if (needsSeparator) {
          sb.append(':');
        }

        sb.append(type.name().toLowerCase());
        needsSeparator = true;
      }

      if (hasName) {
        if (needsSeparator) {
          sb.append(':');
        }

        if (hasParent) {
          sb.append(parent).append('/');
        }

        sb.append(name);
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
