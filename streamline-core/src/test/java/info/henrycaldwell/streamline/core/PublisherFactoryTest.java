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

public class PublisherFactoryTest {

  @Nested
  class Validate {

    @Test
    void doesNotThrowOnValidConfig() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = no_op
          """);

      assertTrue(PublisherFactory.validate(config, 0).isEmpty());
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = PublisherFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          """);

      SpecException exception = PublisherFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = unknown
          """);

      SpecException exception = PublisherFactory.validate(config, 0).get(0);

      assertTrue(exception.getMessage().contains("Unknown publisher type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }

    @Test
    void accumulatesBaseAndConcreteErrors() {
      Config config = ConfigFactory.parseString("""
          type = instagram
          """);

      List<SpecException> exceptions = PublisherFactory.validate(config, 0);

      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=name")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=accountId")));
      assertTrue(exceptions.stream().anyMatch(e -> e.getMessage().contains("Missing required key")
          && e.getMessage().contains("index=0") && e.getMessage().contains("key=accessKey")));
    }
  }

  @Nested
  class FromConfig {

    @Test
    void returnsPublisher() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = no_op
          """);

      assertDoesNotThrow(() -> PublisherFactory.fromConfig(config, 0));
    }

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = no_op
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PublisherFactory.fromConfig(config, 0));

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

      SpecException exception = assertThrows(SpecException.class, () -> PublisherFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PublisherFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = publisher
          type = ""
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PublisherFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnUnknownType() {
      Config config = ConfigFactory.parseString("""
          name = unknown_publisher
          type = unknown
          """);

      SpecException exception = assertThrows(SpecException.class, () -> PublisherFactory.fromConfig(config, 0));

      assertTrue(exception.getMessage().contains("Unknown publisher type"));
      assertTrue(exception.getMessage().contains("index=0"));
      assertTrue(exception.getMessage().contains("type=unknown"));
    }
  }
}
