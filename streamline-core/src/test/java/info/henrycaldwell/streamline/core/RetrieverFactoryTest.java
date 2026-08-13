package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.error.SpecException;

public class RetrieverFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = no_op
          """);

      assertTrue(RetrieverFactory.validate(config, 0).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = RetrieverFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          """);

      SpecException exception = RetrieverFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = unknown
          """);

      SpecException exception = RetrieverFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Unknown retriever type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = twitch
          """);

      List<SpecException> exceptions = RetrieverFactory.validate(config, 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=clientId")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=accessKey")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsRetriever() {
      Config config = ConfigFactory.parseString("""
          name = retriever
          type = no_op
          """);

      assertDoesNotThrow(() -> RetrieverFactory.fromConfig(config, 0));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_retriever
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_retriever
          type = ""
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_retriever
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Unknown retriever type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
