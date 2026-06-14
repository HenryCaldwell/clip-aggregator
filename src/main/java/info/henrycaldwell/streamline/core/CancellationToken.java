package info.henrycaldwell.streamline.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for signaling run cancellation across components.
 * 
 * This class carries a cancellation reason and cancels registered cancellables
 * on the cancel call.
 */
public final class CancellationToken {

  private final AtomicReference<CancellationReason> reason = new AtomicReference<>();
  private final Set<Cancellable> cancellables = ConcurrentHashMap.newKeySet();

  /**
   * Constructs a CancellationToken.
   */
  public CancellationToken() {
  }

  /**
   * Registers the input cancellable to be canceled on cancellation.
   *
   * @param cancellable A {@link Cancellable} representing the resource to
   *                    register.
   */
  public void register(Cancellable cancellable) {
    cancellables.add(cancellable);

    if (reason.get() != null) {
      cancellable.cancel();
    }
  }

  /**
   * Unregisters the input cancellable from cancellation tracking.
   *
   * @param cancellable A {@link Cancellable} representing the resource to
   *                    unregister.
   */
  public void unregister(Cancellable cancellable) {
    cancellables.remove(cancellable);
  }

  /**
   * Signals cancellation with the provided cancellation reason.
   *
   * @param reason A {@link CancellationReason} representing the cause.
   */
  public void cancel(CancellationReason reason) {
    if (this.reason.compareAndSet(null, reason)) {
      for (Cancellable cancellable : cancellables) {
        cancellable.cancel();
      }
    }
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
