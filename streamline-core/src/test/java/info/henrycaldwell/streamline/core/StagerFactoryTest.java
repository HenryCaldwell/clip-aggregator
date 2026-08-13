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

public class StagerFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          """);

      assertTrue(StagerFactory.validate(config).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = StagerFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = stager
          """);

      SpecException exception = StagerFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = unknown
          """);

      SpecException exception = StagerFactory.validate(config).get(0);

      assertTrue(exception.getMessage().contains("Unknown stager type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = "cloudflare-r2"
          """);

      List<SpecException> exceptions = StagerFactory.validate(config);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("key=accountId")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsStager() {
      Config config = ConfigFactory.parseString("""
          name = stager
          type = no_op
          """);

      assertDoesNotThrow(() -> StagerFactory.fromConfig(config));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_stager
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = no_op_stager
          type = ""
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_stager
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Unknown stager type"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
