package info.henrycaldwell.streamline.error;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Class for representing a batch of peer exceptions.
 *
 * This class reports failures accumulated during a single operation as a
 * group of peer exceptions.
 */
public final class AggregateException extends AbstractException {

  private final List<AbstractException> exceptions;

  /**
   * Constructs an AggregateException.
   *
   * @param exceptions A {@link List} of {@link AbstractException} representing
   *                   the contained exceptions.
   * @throws IllegalArgumentException if exceptions is null or empty.
   */
  public AggregateException(List<? extends AbstractException> exceptions) {
    super(null, null, null, null, format(exceptions));

    this.exceptions = List.copyOf(exceptions);
  }

  /**
   * Prints each contained exception to the given stream.
   *
   * @param stream A {@link PrintStream} representing the output stream.
   */
  @Override
  public void printStackTrace(PrintStream stream) {
    for (AbstractException exception : exceptions) {
      exception.printStackTrace(stream);
      stream.println();
    }
  }

  /**
   * Prints each contained exception to the given writer.
   *
   * @param writer A {@link PrintWriter} representing the output writer.
   */
  @Override
  public void printStackTrace(PrintWriter writer) {
    for (AbstractException exception : exceptions) {
      exception.printStackTrace(writer);
      writer.println();
    }
  }

  /**
   * Returns the contained exceptions.
   *
   * @return A {@link List} of {@link AbstractException} representing the
   *         individual exceptions.
   */
  public List<AbstractException> getExceptions() {
    return exceptions;
  }

  /**
   * Formats the given exceptions into a composite message.
   *
   * @param exceptions A {@link List} of {@link AbstractException} representing
   *                   the contained exceptions.
   * @return A string representing the composite message.
   * @throws IllegalArgumentException if exceptions is null or empty.
   */
  private static String format(List<? extends AbstractException> exceptions) {
    if (exceptions == null) {
      throw new IllegalArgumentException("exceptions must be provided");
    }

    if (exceptions.isEmpty()) {
      throw new IllegalArgumentException("exceptions must not be empty");
    }

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < exceptions.size(); i++) {
      if (i > 0) {
        sb.append("\n");
      }

      sb.append(exceptions.get(i).getMessage());
    }

    return sb.toString();
  }
}
