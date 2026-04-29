package info.henrycaldwell.aggregator.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class StagerFactoryTest {

  @Nested
  class FromConfig {

    @Test
    void throwsOnMissingName() {
      Config config = ConfigFactory.parseString("""
          type = cloudflare-r2
          accountId = account
          accessKey = access
          secretKey = secret
          bucket = bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnBlankName() {
      Config config = ConfigFactory.parseString("""
          name = ""
          type = cloudflare-r2
          accountId = account
          accessKey = access
          secretKey = secret
          bucket = bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=name"));
    }

    @Test
    void throwsOnMissingType() {
      Config config = ConfigFactory.parseString("""
          name = r2_stager
          accountId = account
          accessKey = access
          secretKey = secret
          bucket = bucket
          publicUrl = "https://cdn.example.com"
          """);

      SpecException exception = assertThrows(SpecException.class, () -> StagerFactory.fromConfig(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=type"));
    }

    @Test
    void throwsOnBlankType() {
      Config config = ConfigFactory.parseString("""
          name = r2_stager
          type = ""
          accountId = account
          accessKey = access
          secretKey = secret
          bucket = bucket
          publicUrl = "https://cdn.example.com"
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
