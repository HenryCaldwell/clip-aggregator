package info.henrycaldwell.streamline.retrieve;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.core.CancellationToken;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.SpecException;

public class NoOpRetrieverTest {

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = no_op
          """);

      assertDoesNotThrow(() -> new NoOpRetriever(config));
    }

    @Test
    void throwsOnUnknownKey() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = no_op
          extra = value
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new NoOpRetriever(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Fetch {

    @Test
    void returnsEmptyList() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = no_op
          """);
      NoOpRetriever retriever = new NoOpRetriever(config);

      List<ClipRef> result = retriever.fetch(new CancellationToken());

      assertEquals(List.of(), result);
    }
  }
}
