package info.henrycaldwell.streamline.observe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpObserverTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpObserver(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpObserver(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class RunStart {

    @Test
    void returnsZero() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertEquals(0L, observer.runStart("runner", null));
    }
  }

  @Nested
  class RunEnd {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertDoesNotThrow(() -> observer.runEnd(0L, RunStatus.SUCCESS, 0));
    }
  }

  @Nested
  class FetchStart {

    @Test
    void returnsZero() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertEquals(0L, observer.fetchStart(0L, "retriever"));
    }
  }

  @Nested
  class FetchEnd {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertDoesNotThrow(() -> observer.fetchEnd(0L, AttemptStatus.SUCCESS, 0, null));
    }
  }

  @Nested
  class AttemptStart {

    @Test
    void returnsZero() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertEquals(0L, observer.attemptStart(0L, "worker", CLIP, PipelineStage.DOWNLOAD, "component"));
    }
  }

  @Nested
  class AttemptEnd {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertDoesNotThrow(() -> observer.attemptEnd(0L, AttemptStatus.SUCCESS, null));
    }
  }

  @Nested
  class Publish {

    @Test
    void doesNothing() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);
      NoOpObserver observer = new NoOpObserver(config);

      assertDoesNotThrow(() -> observer.publish(0L, "clip-1", "publisher", "https://example.com/p/1"));
    }
  }
}
