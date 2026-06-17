package info.henrycaldwell.streamline.retrieve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.SpecException;

public class AbstractRetrieverTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          """);

      assertDoesNotThrow(() -> new TestRetriever(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = test
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestRetriever(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestRetriever(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnWrongTypeForPipeline() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          pipeline = [pipeline]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestRetriever(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=pipeline"));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new TestRetriever(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class GetName {

    @Test
    void returnsConfiguredName() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          """);
      TestRetriever retriever = new TestRetriever(config);

      String result = retriever.getName();

      assertEquals("retriever", result);
    }
  }

  @Nested
  class GetPipeline {

    @Test
    void returnsNullWhenPipelineIsMissing() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          """);
      TestRetriever retriever = new TestRetriever(config);

      String result = retriever.getPipeline();

      assertNull(result);
    }

    @Test
    void returnsNullWhenPipelineIsBlank() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          pipeline = ""
          """);
      TestRetriever retriever = new TestRetriever(config);

      String result = retriever.getPipeline();

      assertNull(result);
    }

    @Test
    void returnsConfiguredPipeline() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = test
          pipeline = pipeline
          """);
      TestRetriever retriever = new TestRetriever(config);

      String result = retriever.getPipeline();

      assertEquals("pipeline", result);
    }
  }

  private static final class TestRetriever extends AbstractRetriever {

    private TestRetriever(Config config) {
      super(config, Spec.builder().build());
    }

    @Override
    public List<ClipRef> fetch(CancellationToken token) {
      return List.of();
    }
  }
}
