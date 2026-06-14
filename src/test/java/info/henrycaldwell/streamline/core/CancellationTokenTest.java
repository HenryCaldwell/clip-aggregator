package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CancellationTokenTest {

  @Nested
  class Register {

    @Test
    void firesImmediatelyIfAlreadyCanceled() {
      AtomicInteger calls = new AtomicInteger();
      CancellationToken token = new CancellationToken();
      token.cancel(CancellationReason.USER_CANCELED);

      token.register(calls::incrementAndGet);

      assertEquals(1, calls.get());
    }
  }

  @Nested
  class Unregister {

    @Test
    void doesNotFireUnregisteredCancellable() {
      AtomicInteger calls = new AtomicInteger();
      CancellationToken token = new CancellationToken();
      Cancellable cancellable = calls::incrementAndGet;
      token.register(cancellable);
      token.unregister(cancellable);

      token.cancel(CancellationReason.USER_CANCELED);

      assertEquals(0, calls.get());
    }
  }

  @Nested
  class AwaitCancellation {

    @Test
    void returnsFalseOnTimeout() throws InterruptedException {
      CancellationToken token = new CancellationToken();

      boolean result = token.awaitCancellation(0);

      assertFalse(result);
    }

    @Test
    void returnsTrueWhenAlreadyCanceled() throws InterruptedException {
      CancellationToken token = new CancellationToken();
      token.cancel(CancellationReason.USER_CANCELED);

      boolean result = token.awaitCancellation(0);

      assertTrue(result);
    }

    @Test
    void wakesWaiterOnCancel() throws InterruptedException {
      CancellationToken token = new CancellationToken();
      Thread canceler = new Thread(() -> {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        token.cancel(CancellationReason.USER_CANCELED);
      });
      canceler.start();

      boolean result = token.awaitCancellation(10);

      assertTrue(result);
    }
  }

  @Nested
  class Cancel {

    @Test
    void setsReasonOnFirstCall() {
      CancellationToken token = new CancellationToken();

      token.cancel(CancellationReason.USER_CANCELED);

      assertEquals(CancellationReason.USER_CANCELED, token.getReason());
    }

    @Test
    void keepsFirstReasonOnSubsequentCalls() {
      CancellationToken token = new CancellationToken();

      token.cancel(CancellationReason.USER_CANCELED);
      token.cancel(CancellationReason.POSTS_REACHED);

      assertEquals(CancellationReason.USER_CANCELED, token.getReason());
    }

    @Test
    void firesRegisteredCancellable() {
      AtomicInteger calls = new AtomicInteger();
      CancellationToken token = new CancellationToken();
      token.register(calls::incrementAndGet);

      token.cancel(CancellationReason.USER_CANCELED);

      assertEquals(1, calls.get());
    }

    @Test
    void firesAllRegisteredCancellables() {
      AtomicInteger calls = new AtomicInteger();
      CancellationToken token = new CancellationToken();
      token.register(calls::incrementAndGet);
      token.register(calls::incrementAndGet);
      token.register(calls::incrementAndGet);

      token.cancel(CancellationReason.USER_CANCELED);

      assertEquals(3, calls.get());
    }

    @Test
    void firesCancellablesOnlyOnce() {
      AtomicInteger calls = new AtomicInteger();
      CancellationToken token = new CancellationToken();
      token.register(calls::incrementAndGet);

      token.cancel(CancellationReason.USER_CANCELED);
      token.cancel(CancellationReason.POSTS_REACHED);

      assertEquals(1, calls.get());
    }
  }

  @Nested
  class GetReason {

    @Test
    void returnsNullBeforeCancel() {
      CancellationToken token = new CancellationToken();

      assertNull(token.getReason());
    }

    @Test
    void returnsReasonAfterCancel() {
      CancellationToken token = new CancellationToken();

      token.cancel(CancellationReason.USER_CANCELED);

      assertEquals(CancellationReason.USER_CANCELED, token.getReason());
    }
  }
}
