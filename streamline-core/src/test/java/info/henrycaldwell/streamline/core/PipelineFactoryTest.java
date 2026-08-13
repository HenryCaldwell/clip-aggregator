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

public class PipelineFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          transformers = [
            {
              name = step
              type = no_op
            }
          ]
          """);

      assertTrue(PipelineFactory.validate(config, 0).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          transformers = []
          """);

      SpecException exception = PipelineFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingTransformers() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          """);

      SpecException exception = PipelineFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnWrongTypeForTransformers() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          transformers = invalid
          """);

      SpecException exception = PipelineFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnDuplicateTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          transformers = [
            { name = step, type = no_op }
            { name = step, type = no_op }
          ]
          """);

      List<SpecException> exceptions = PipelineFactory.validate(config, 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Duplicate transformer name")
          && e.getMessage().contains("index=1") && e.getMessage().contains("name=step")));
    }

    @Test
    void accumulatesNestedTransformerErrors() {
      Config config = ConfigFactory.parseString("""
          name = pipeline
          transformers = [
            {
              type = no_op
            }
          ]
          """);

      List<SpecException> exceptions = PipelineFactory.validate(config, 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")));
    }

    @Test
    void accumulatesPipelineAndTransformerErrors() {
      Config config = ConfigFactory.parseString("""
          transformers = [
            {
              type = no_op
            }
          ]
          """);

      List<SpecException> exceptions = PipelineFactory.validate(config, 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")
          && e.getMessage().contains("UNNAMED_PIPELINE")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")
          && e.getMessage().contains("UNNAMED_TRANSFORMER")));
    }
  }

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

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnDuplicateTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = step
              type = no_op
            }
            {
              name = step
              type = no_op
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Duplicate transformer name"));
      assertTrue(exception.getMessage().contains("index=1"));
      assertTrue(exception.getMessage().contains("name=step"));
    }
  }
}
