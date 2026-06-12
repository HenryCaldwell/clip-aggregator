package info.henrycaldwell.streamline.core;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for signaling run cancellation across components.
 *
 * This class carries a cancellation reason set on the first cancel call.
 */
public final class CancellationToken {

  private final AtomicReference<CancellationReason> reason = new AtomicReference<>();

  /**
   * Constructs a CancellationToken.
   */
  public CancellationToken() {
  }

  /**
   * Signals cancellation with the provided cancellation reason.
   *
   * @param reason A {@link CancellationReason} representing the cause.
   */
  public void cancel(CancellationReason reason) {
    this.reason.compareAndSet(null, reason);
  }

  /**
   * Returns the cancellation reason.
   *
   * @return A {@link CancellationReason} representing the cause, or {@code null}.
   */
  public CancellationReason getReason() {
    return reason.get();
  }
}
