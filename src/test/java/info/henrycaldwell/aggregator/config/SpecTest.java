package info.henrycaldwell.aggregator.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
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

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnMissingRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredNumber("count")
            .build();

        Config config = ConfigFactory.parseString("");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnMissingRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredNumber("enabled")
            .build();

        Config config = ConfigFactory.parseString("");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnMissingRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnMissingRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnMissingRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredString() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = [1, 2, 3]");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredString("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredString("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
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

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumber() {
        Spec spec = Spec.builder()
            .optionalNumber("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBoolean() {
        Spec spec = Spec.builder()
            .optionalBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalStringList() {
        Spec spec = Spec.builder()
            .optionalStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumberList() {
        Spec spec = Spec.builder()
            .optionalNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBooleanList() {
        Spec spec = Spec.builder()
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        assertThrows(SpecException.class, () -> spec.validate(config, "test"));
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

      assertThrows(SpecException.class, () -> union.validate(specAConfig, "test"));
      assertThrows(SpecException.class, () -> union.validate(specBConfig, "test"));
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

      assertThrows(SpecException.class, () -> union.validate(config, "test"));
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
