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

public class TransformerFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = no_op
          """);

      assertTrue(TransformerFactory.validate(config, "test_pipeline", 0).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = TransformerFactory.validate(config, "test_pipeline", 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          """);

      SpecException exception = TransformerFactory.validate(config, "test_pipeline", 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = unknown
          """);

      SpecException exception = TransformerFactory.validate(config, "test_pipeline", 0).get(0);

      assertTrue(exception.getMessage().contains("Unknown transformer type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = watermark
          """);

      List<SpecException> exceptions = TransformerFactory.validate(config, "test_pipeline", 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=fontPath")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsTransformer() {
      Config config = ConfigFactory.parseString("""
          name = transformer
          type = no_op
          """);

      assertDoesNotThrow(() -> TransformerFactory.fromConfig(config, "test_pipeline", 0));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class,
          () -> TransformerFactory.fromConfig(config, "test_pipeline", 0));

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

      SpecException exception = assertThrows(SpecException.class,
          () -> TransformerFactory.fromConfig(config, "test_pipeline", 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_transformer
          """);

      SpecException exception = assertThrows(SpecException.class,
          () -> TransformerFactory.fromConfig(config, "test_pipeline", 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_transformer
          type = ""
          """);

      SpecException exception = assertThrows(SpecException.class,
          () -> TransformerFactory.fromConfig(config, "test_pipeline", 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_transformer
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class,
          () -> TransformerFactory.fromConfig(config, "test_pipeline", 0));

      assertTrue(exception.getMessage().contains("Unknown transformer type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
