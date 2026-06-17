package info.henrycaldwell.streamline.transform;

import java.io.IOException;

/**
 * Functional interface for starting a transformation subprocess.
 *
 * This interface defines a contract for starting the subprocess used by
 * transformers, allowing the real subprocess to be substituted in tests.
 */
@FunctionalInterface
interface ProcessFactory {

  /**
   * Starts a transformation subprocess from the given process builder.
   *
   * @param pb A {@link ProcessBuilder} representing the subprocess to start.
   * @return A {@link Process} representing the started subprocess.
   * @throws IOException if the subprocess fails to start.
   */
  Process start(ProcessBuilder pb) throws IOException;
}
