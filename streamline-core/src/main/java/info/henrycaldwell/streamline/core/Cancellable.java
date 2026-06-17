package info.henrycaldwell.streamline.core;

/**
 * Functional interface for cancellation actions.
 * 
 * This interface defines a contract for aborting an in-flight resource.
 */
@FunctionalInterface
public interface Cancellable {

  /**
   * Cancels the underlying resource.
   */
  void cancel();
}
