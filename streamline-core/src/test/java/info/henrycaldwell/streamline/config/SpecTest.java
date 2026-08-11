package info.henrycaldwell.streamline.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.error.ComponentType;
import info.henrycaldwell.streamline.error.SpecException;

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

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Unknown configuration key"));
        assertTrue(exception.getMessage().contains("key=unknown"));
      }

      @Test
      void doesNotThrowWithOnlyKnownKeys() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = test");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
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

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnBlankRequiredString() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = \"\"");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnMissingRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredNumber("count")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnMissingRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnMissingRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnMissingRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnMissingRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=flags"));
      }

      @Test
      void throwsOnWrongTypeForRequiredString() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .build();

        Config config = ConfigFactory.parseString("name = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumber() {
        Spec spec = Spec.builder()
            .requiredNumber("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBoolean() {
        Spec spec = Spec.builder()
            .requiredBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected boolean)"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnWrongTypeForRequiredStringList() {
        Spec spec = Spec.builder()
            .requiredStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnWrongTypeForRequiredNumberList() {
        Spec spec = Spec.builder()
            .requiredNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<number>)"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnWrongTypeForRequiredBooleanList() {
        Spec spec = Spec.builder()
            .requiredBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

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

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
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

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowWhenOptionalNumberMissing() {
        Spec spec = Spec.builder()
            .optionalNumber("count")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowWhenOptionalBooleanMissing() {
        Spec spec = Spec.builder()
            .optionalBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowWhenOptionalStringListMissing() {
        Spec spec = Spec.builder()
            .optionalStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowWhenOptionalNumberListMissing() {
        Spec spec = Spec.builder()
            .optionalNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowWhenOptionalBooleanListMissing() {
        Spec spec = Spec.builder()
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void throwsOnWrongTypeForOptionalString() {
        Spec spec = Spec.builder()
            .optionalString("name")
            .build();

        Config config = ConfigFactory.parseString("name = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
        assertTrue(exception.getMessage().contains("key=name"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumber() {
        Spec spec = Spec.builder()
            .optionalNumber("count")
            .build();

        Config config = ConfigFactory.parseString("count = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected number)"));
        assertTrue(exception.getMessage().contains("key=count"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBoolean() {
        Spec spec = Spec.builder()
            .optionalBoolean("enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected boolean)"));
        assertTrue(exception.getMessage().contains("key=enabled"));
      }

      @Test
      void throwsOnWrongTypeForOptionalStringList() {
        Spec spec = Spec.builder()
            .optionalStringList("tags")
            .build();

        Config config = ConfigFactory.parseString("tags = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<string>)"));
        assertTrue(exception.getMessage().contains("key=tags"));
      }

      @Test
      void throwsOnWrongTypeForOptionalNumberList() {
        Spec spec = Spec.builder()
            .optionalNumberList("counts")
            .build();

        Config config = ConfigFactory.parseString("counts = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<number>)"));
        assertTrue(exception.getMessage().contains("key=counts"));
      }

      @Test
      void throwsOnWrongTypeForOptionalBooleanList() {
        Spec spec = Spec.builder()
            .optionalBooleanList("flags")
            .build();

        Config config = ConfigFactory.parseString("flags = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

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

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }
    }

    @Nested
    class KeyGroups {

      @Test
      void throwsOnEmptyGroupForExactlyOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .exactlyOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key combination (expected exactly one of a, b)"));
        assertTrue(exception.getMessage().contains("count=0"));
      }

      @Test
      void throwsOnMultipleKeysForExactlyOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .exactlyOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1, b = 2");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key combination (expected exactly one of a, b)"));
        assertTrue(exception.getMessage().contains("count=2"));
      }

      @Test
      void doesNotThrowOnSingleKeyForExactlyOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .exactlyOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void throwsOnEmptyGroupForAtLeastOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .atLeastOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key combination (expected at least one of a, b)"));
        assertTrue(exception.getMessage().contains("count=0"));
      }

      @Test
      void doesNotThrowOnSingleKeyForAtLeastOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .atLeastOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowOnAllKeysForAtLeastOne() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .atLeastOne("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1, b = 2");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void throwsOnPartialGroupForMutuallyInclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyInclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key combination (expected all or none of a, b)"));
        assertTrue(exception.getMessage().contains("count=1"));
      }

      @Test
      void doesNotThrowOnEmptyGroupForMutuallyInclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyInclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowOnAllKeysForMutuallyInclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyInclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1, b = 2");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void throwsOnMultipleKeysForMutuallyExclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyExclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1, b = 2");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key combination (expected at most one of a, b)"));
        assertTrue(exception.getMessage().contains("count=2"));
      }

      @Test
      void doesNotThrowOnEmptyGroupForMutuallyExclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyExclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }

      @Test
      void doesNotThrowOnSingleKeyForMutuallyExclusive() {
        Spec spec = Spec.builder()
            .optionalString("a", "b")
            .mutuallyExclusive("a", "b")
            .build();

        Config config = ConfigFactory.parseString("a = 1");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
      }
    }

    @Nested
    class ConstraintChecks {

      @Test
      void throwsOnConstraintViolationForRequiredString() {
        StringConstraint constraint = new TestStringConstraint(value -> false, "allowed");
        Spec spec = Spec.builder()
            .requiredString(constraint, "name")
            .build();

        Config config = ConfigFactory.parseString("name = c");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("allowed"));
        assertTrue(exception.getMessage().contains("key=name"));
        assertTrue(exception.getMessage().contains("value=c"));
      }

      @Test
      void throwsOnConstraintViolationForRequiredNumber() {
        NumberConstraint constraint = new TestNumberConstraint(value -> false, "positive");
        Spec spec = Spec.builder()
            .requiredNumber(constraint, "count")
            .build();

        Config config = ConfigFactory.parseString("count = 0");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("positive"));
        assertTrue(exception.getMessage().contains("key=count"));
        assertTrue(exception.getMessage().contains("value=0"));
      }

      @Test
      void throwsOnConstraintViolationForRequiredBoolean() {
        BooleanConstraint constraint = new TestBooleanConstraint(value -> false, "true");
        Spec spec = Spec.builder()
            .requiredBoolean(constraint, "enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = false");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("true"));
        assertTrue(exception.getMessage().contains("key=enabled"));
        assertTrue(exception.getMessage().contains("value=false"));
      }

      @Test
      void throwsOnConstraintViolationForRequiredStringList() {
        StringListConstraint constraint = new TestStringListConstraint(value -> false, "allowed");
        Spec spec = Spec.builder()
            .requiredStringList(constraint, "tags")
            .build();

        Config config = ConfigFactory.parseString("tags = [\"a\", \"\", \"c\"]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("allowed"));
        assertTrue(exception.getMessage().contains("key=tags"));
        assertTrue(exception.getMessage().contains("value=[a, , c]"));
      }

      @Test
      void throwsOnConstraintViolationForRequiredNumberList() {
        NumberListConstraint constraint = new TestNumberListConstraint(value -> false, "positive");
        Spec spec = Spec.builder()
            .requiredNumberList(constraint, "counts")
            .build();

        Config config = ConfigFactory.parseString("counts = [1, 0, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("positive"));
        assertTrue(exception.getMessage().contains("key=counts"));
        assertTrue(exception.getMessage().contains("value=[1, 0, 3]"));
      }

      @Test
      void throwsOnConstraintViolationForRequiredBooleanList() {
        BooleanListConstraint constraint = new TestBooleanListConstraint(value -> false, "true");
        Spec spec = Spec.builder()
            .requiredBooleanList(constraint, "flags")
            .build();

        Config config = ConfigFactory.parseString("flags = [true, false, true]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("true"));
        assertTrue(exception.getMessage().contains("key=flags"));
        assertTrue(exception.getMessage().contains("value=[true, false, true]"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalString() {
        StringConstraint constraint = new TestStringConstraint(value -> false, "allowed");
        Spec spec = Spec.builder()
            .optionalString(constraint, "name")
            .build();

        Config config = ConfigFactory.parseString("name = c");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("allowed"));
        assertTrue(exception.getMessage().contains("key=name"));
        assertTrue(exception.getMessage().contains("value=c"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalNumber() {
        NumberConstraint constraint = new TestNumberConstraint(value -> false, "positive");
        Spec spec = Spec.builder()
            .optionalNumber(constraint, "count")
            .build();

        Config config = ConfigFactory.parseString("count = 0");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("positive"));
        assertTrue(exception.getMessage().contains("key=count"));
        assertTrue(exception.getMessage().contains("value=0"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalBoolean() {
        BooleanConstraint constraint = new TestBooleanConstraint(value -> false, "true");
        Spec spec = Spec.builder()
            .optionalBoolean(constraint, "enabled")
            .build();

        Config config = ConfigFactory.parseString("enabled = false");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("true"));
        assertTrue(exception.getMessage().contains("key=enabled"));
        assertTrue(exception.getMessage().contains("value=false"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalStringList() {
        StringListConstraint constraint = new TestStringListConstraint(value -> false, "allowed");
        Spec spec = Spec.builder()
            .optionalStringList(constraint, "tags")
            .build();

        Config config = ConfigFactory.parseString("tags = [\"a\", \"\", \"c\"]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("allowed"));
        assertTrue(exception.getMessage().contains("key=tags"));
        assertTrue(exception.getMessage().contains("value=[a, , c]"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalNumberList() {
        NumberListConstraint constraint = new TestNumberListConstraint(value -> false, "positive");
        Spec spec = Spec.builder()
            .optionalNumberList(constraint, "counts")
            .build();

        Config config = ConfigFactory.parseString("counts = [1, 0, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("positive"));
        assertTrue(exception.getMessage().contains("key=counts"));
        assertTrue(exception.getMessage().contains("value=[1, 0, 3]"));
      }

      @Test
      void throwsOnConstraintViolationForOptionalBooleanList() {
        BooleanListConstraint constraint = new TestBooleanListConstraint(value -> false, "true");
        Spec spec = Spec.builder()
            .optionalBooleanList(constraint, "flags")
            .build();

        Config config = ConfigFactory.parseString("flags = [true, false, true]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("true"));
        assertTrue(exception.getMessage().contains("key=flags"));
        assertTrue(exception.getMessage().contains("value=[true, false, true]"));
      }

      @Test
      void doesNotThrowWhenAllConstraintsPass() {
        Spec spec = Spec.builder()
            .requiredNumber(new TestNumberConstraint(value -> true, "positive"), "count")
            .requiredString(new TestStringConstraint(value -> true, "allowed"), "name")
            .requiredNumberList(new TestNumberListConstraint(value -> true, "positive"), "counts")
            .build();

        Config config = ConfigFactory.parseString("count = 1, name = \"hi\", counts = [1, 2, 3]");

        assertTrue(spec.validate(config, ComponentType.ROOT, null, "test").isEmpty());
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

      SpecException missingCount = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingName = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingCount.getMessage().contains("Missing required key"));
      assertTrue(missingCount.getMessage().contains("key=count"));
      assertTrue(missingName.getMessage().contains("Missing required key"));
      assertTrue(missingName.getMessage().contains("key=name"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
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

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
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

      SpecException exception = union.validate(config, ComponentType.ROOT, null, "test").get(0);

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=unknown"));
    }

    @Test
    void unionOfEmptySpecsDoesNotThrowOnEmptyConfig() {
      Spec specA = Spec.builder().build();
      Spec specB = Spec.builder().build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void keyGroupsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .optionalString("a", "b")
          .exactlyOne("a", "b")
          .build();

      Spec specB = Spec.builder()
          .optionalString("c", "d")
          .mutuallyExclusive("c", "d")
          .build();

      Spec union = Spec.union(specA, specB);

      Config valid = ConfigFactory.parseString("a = 1");
      Config specAViolation = ConfigFactory.parseString("a = 1, b = 2, c = 3");
      Config specBViolation = ConfigFactory.parseString("a = 1, c = 3, d = 4");

      SpecException exactlyOneViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException mutuallyExclusiveViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(exactlyOneViolation.getMessage().contains("Invalid key combination (expected exactly one of a, b)"));
      assertTrue(mutuallyExclusiveViolation.getMessage().contains("Invalid key combination (expected at most one of c, d)"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }
  }

  private static final class TestStringConstraint implements StringConstraint {

    private final Predicate<String> predicate;
    private final String description;

    private TestStringConstraint(Predicate<String> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(String value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestNumberConstraint implements NumberConstraint {

    private final Predicate<Number> predicate;
    private final String description;

    private TestNumberConstraint(Predicate<Number> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(Number value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestBooleanConstraint implements BooleanConstraint {

    private final Predicate<Boolean> predicate;
    private final String description;

    private TestBooleanConstraint(Predicate<Boolean> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(Boolean value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestStringListConstraint implements StringListConstraint {

    private final Predicate<List<String>> predicate;
    private final String description;

    private TestStringListConstraint(Predicate<List<String>> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(List<String> value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestNumberListConstraint implements NumberListConstraint {

    private final Predicate<List<Number>> predicate;
    private final String description;

    private TestNumberListConstraint(Predicate<List<Number>> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(List<Number> value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestBooleanListConstraint implements BooleanListConstraint {

    private final Predicate<List<Boolean>> predicate;
    private final String description;

    private TestBooleanListConstraint(Predicate<List<Boolean>> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(List<Boolean> value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }
}
