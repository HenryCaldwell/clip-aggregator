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

public class AbstractObserverTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = test
          """);

      assertDoesNotThrow(() -> new TestObserver(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestObserver(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = observer
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestObserver(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestObserver(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = test
          """);
      TestObserver observer = new TestObserver(config);

      assertDoesNotThrow(observer::start);
    }
  }

  @Nested
  class Stop {

    @Test
    void doesNothingByDefault() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = test
          """);
      TestObserver observer = new TestObserver(config);

      assertDoesNotThrow(observer::stop);
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = test
          """);
      TestObserver observer = new TestObserver(config);

      String result = observer.getName();

      assertEquals("observer", result);
    }
  }

  private static final class TestObserver extends AbstractObserver {

    private TestObserver(Config config) {
      super(config);
    }

    @Override
    public long runStart(String runner, String config) {
      return 0L;
    }

    @Override
    public void runEnd(long runId, RunStatus status, int published) {
    }

    @Override
    public long fetchStart(long runId, String retriever, String worker) {
      return 0L;
    }

    @Override
    public void fetchEnd(long fetchId, AttemptStatus status, int clips, Throwable error) {
    }

    @Override
    public long attemptStart(long runId, ClipRef clip, PipelineStage stage, String component, String worker) {
      return 0L;
    }

    @Override
    public void attemptEnd(long attemptId, AttemptStatus status, Throwable error) {
    }

    @Override
    public void publish(long runId, String clipId, String publisher, String uri) {
    }
  }
}
