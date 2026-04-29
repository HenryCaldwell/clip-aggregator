package info.henrycaldwell.aggregator.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.SpecException;

public class SpecTest {

  @Nested
  class Validate {

    @Nested
    class UnknownKeys {

      @Test
      void throwsOnUnknownKey() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = test, unknown = value");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Unknown configuration key"));
        assertTrue(exception.getMessage().contains("key=unknown"));
      }

      @Test
      void doesNotThrowWithOnlyKnownKeys() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = test");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }
    }

    @Nested
    class RequiredKeys {

      @Test
      void throwsOnMissingRequiredString() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnMissingRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredNumber("count")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnMissingRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnMissingRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnMissingRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnMissingRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=flags"));
      }

      @Test
      void throwsOnWrongTypeForRequiredString() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredNumber("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected boolean)"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnWrongTypeForRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<number>)"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<boolean>)"));
        assertTrue(exception.getMessage().contains("key=flags"));
      }

      @Test
      void doesNotThrowWhenAllRequiredKeysPresentWithCorrectTypes() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .requiredNumber("count")
            .requiredBoolean("enabled")
            .requiredStringList("tags")
            .requiredNumberList("counts")
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString(
            "name = test, count = 1, enabled = true, tags = [a, b], counts = [1, 2], flags = [true, false]");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }
    }

    @Nested
    class OptionalKeys {

      @Test
      void doesNotThrowWhenOptionalStringMissing() {
        Spec spec = Spec.builder()
            .optionalString("name")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void doesNotThrowWhenOptionalNumberMissing() {
        Spec spec = Spec.builder()
            .optionalNumber("count")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void doesNotThrowWhenOptionalBooleanMissing() {
        Spec spec = Spec.builder()
            .optionalBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void doesNotThrowWhenOptionalStringListMissing() {
        Spec spec = Spec.builder()
            .optionalStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void doesNotThrowWhenOptionalNumberListMissing() {
        Spec spec = Spec.builder()
            .optionalNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void doesNotThrowWhenOptionalBooleanListMissing() {
        Spec spec = Spec.builder()
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalString() {
        Spec spec = Spec.builder()
            .optionalString("name")
            .build();

        Config config = ConfigFactory.parseString("name = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumber() {
        Spec spec = Spec.builder()
            .optionalNumber("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBoolean() {
        Spec spec = Spec.builder()
            .optionalBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected boolean)"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnWrongTypeForOptionalStringList() {
        Spec spec = Spec.builder()
            .optionalStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumberList() {
        Spec spec = Spec.builder()
            .optionalNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<number>)"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBooleanList() {
        Spec spec = Spec.builder()
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        SpecException exception = assertThrows(SpecException.class, () -> spec.validate(config, "test"));

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<boolean>)"));
        assertTrue(exception.getMessage().contains("key=flags"));
      }

      @Test
      void doesNotThrowWhenAllOptionalKeysPresentWithCorrectTypes() {
        Spec spec = Spec.builder()
            .optionalString("name")
            .optionalNumber("count")
            .optionalBoolean("enabled")
            .optionalStringList("tags")
            .optionalNumberList("counts")
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString(
            "name = test, count = 1, enabled = true, tags = [a, b], counts = [1, 2], flags = [true, false]");

        assertDoesNotThrow(() -> spec.validate(config, "test"));
      }
    }
  }

  @Nested
  class Union {

    @Test
    void requiredKeysFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredString("name")
          .build();

      Spec specB = Spec.builder()
          .requiredNumber("count")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("name = test");
      Config specBConfig = ConfigFactory.parseString("count = 1");
      Config combinedConfig = ConfigFactory.parseString("name = test, count = 1");

      SpecException missingCount = assertThrows(SpecException.class, () -> union.validate(specAConfig, "test"));
      SpecException missingName = assertThrows(SpecException.class, () -> union.validate(specBConfig, "test"));

      assertTrue(missingCount.getMessage().contains("Missing required key"));
      assertTrue(missingCount.getMessage().contains("key=count"));
      assertTrue(missingName.getMessage().contains("Missing required key"));
      assertTrue(missingName.getMessage().contains("key=name"));
      assertDoesNotThrow(() -> union.validate(combinedConfig, "test"));
    }

    @Test
    void optionalKeysFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalString("name")
          .build();

      Spec specB = Spec.builder()
          .optionalNumber("count")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("name = test, count = 1");

      assertDoesNotThrow(() -> union.validate(config, "test"));
    }

    @Test
    void unknownKeysNotInEitherSpecThrow() {
      Spec specA = Spec.builder()
          .requiredString("name")
          .build();

      Spec specB = Spec.builder()
          .requiredNumber("count")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("name = test, count = 1, unknown = value");

      SpecException exception = assertThrows(SpecException.class, () -> union.validate(config, "test"));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=unknown"));
    }

    @Test
    void unionOfEmptySpecsDoesNotThrowOnEmptyConfig() {
      Spec specA = Spec.builder().build();
      Spec specB = Spec.builder().build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("");

      assertDoesNotThrow(() -> union.validate(config, "test"));
    }
  }
}
