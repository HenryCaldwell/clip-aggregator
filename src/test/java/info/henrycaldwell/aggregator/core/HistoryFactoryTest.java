package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;
public class HistoryFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = sqlite
          databasePath = clip-history.db
          """);

      SpecException exception = assertThrows(SpecException.class, () -> HistoryFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = sqlite
          databasePath = clip-history.db
          """);

      SpecException exception = assertThrows(SpecException.class, () -> HistoryFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = sqlite_history
          databasePath = clip-history.db
          """);

      SpecException exception = assertThrows(SpecException.class, () -> HistoryFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = sqlite_history
          type = ""
          databasePath = clip-history.db
          """);

      SpecException exception = assertThrows(SpecException.class, () -> HistoryFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_history
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> HistoryFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown history type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

  }
}
