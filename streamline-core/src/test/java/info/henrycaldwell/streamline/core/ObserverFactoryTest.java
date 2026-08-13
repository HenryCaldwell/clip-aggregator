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

public class ObserverFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);

      assertTrue(ObserverFactory.validate(config).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = ObserverFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = observer
          """);

      SpecException exception = ObserverFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = unknown
          """);

      SpecException exception = ObserverFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Unknown observer type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = sqlite
          """);

      List<SpecException> exceptions = ObserverFactory.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=databasePath")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsObserver() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = no_op
          """);

      assertDoesNotThrow(() -> ObserverFactory.fromConfig(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> ObserverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> ObserverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_observer
          """);

      SpecException exception = assertThrows(SpecException.class, () -> ObserverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_observer
          type = ""
          """);

      SpecException exception = assertThrows(SpecException.class, () -> ObserverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_observer
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> ObserverFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown observer type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
