package info.henrycaldwell.streamline.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.error.SpecException;

public class PipelineFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void returnsPipeline() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          transformers = [
            {
              name = step
              type = no_op
            }
          ]
          """);

      assertDoesNotThrow(() -> PipelineFactory.fromConfig(config, 0));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          transformers = []
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          transformers = []
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingTransformers() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnWrongTypeForTransformers() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list)"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnMissingTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              type = no_op
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = ""
              type = no_op
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingTransformerType() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = no_op_step
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankTransformerType() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = no_op_step
              type = ""
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownTransformerType() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = unknown_type
              type = unknown
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Unknown transformer type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
