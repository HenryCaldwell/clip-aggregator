package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class PipelineFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          transformers = []
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          transformers = []
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingTransformers() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnWrongTypeForTransformers() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = invalid
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected list)"));
      assertTrue(exception.getMessage().contains("key=transformers"));
    }

    @Test
    void throwsOnMissingTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              type = fps
              ffmpegPath = ffmpeg
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankTransformerName() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = ""
              type = fps
              ffmpegPath = ffmpeg
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingTransformerType() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = fps_step
              ffmpegPath = ffmpeg
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankTransformerType() {
      Config config = ConfigFactory.parseString("""
          name = test_pipeline
          transformers = [
            {
              name = fps_step
              type = ""
              ffmpegPath = ffmpeg
            }
          ]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
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

      SpecException exception = assertThrows(SpecException.class, () -> PipelineFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown transformer type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

  }
}
