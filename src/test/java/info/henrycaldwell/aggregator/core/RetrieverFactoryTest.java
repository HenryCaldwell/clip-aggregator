package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;
public class RetrieverFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = twitch
          token = token
          gameId = game
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = twitch
          token = token
          gameId = game
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = twitch_retriever
          token = token
          gameId = game
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = twitch_retriever
          type = ""
          token = token
          gameId = game
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_retriever
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> RetrieverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown retriever type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

  }
}
