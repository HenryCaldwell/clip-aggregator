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
      void throwsOnMissingRequiredObject() {
        Spec spec = Spec.builder()
            .requiredObject("nested")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=nested"));
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
      void throwsOnMissingRequiredObjectList() {
        Spec spec = Spec.builder()
            .requiredObjectList("items")
            .build();

        Config config = ConfigFactory.parseString("");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Missing required key"));
        assertTrue(exception.getMessage().contains("key=items"));
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
      void throwsOnWrongTypeForRequiredObject() {
        Spec spec = Spec.builder()
            .requiredObject("nested")
            .build();

        Config config = ConfigFactory.parseString("nested = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
        assertTrue(exception.getMessage().contains("key=nested"));
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
      void throwsOnWrongTypeForRequiredObjectList() {
        Spec spec = Spec.builder()
            .requiredObjectList("items")
            .build();

        Config config = ConfigFactory.parseString("items = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
        assertTrue(exception.getMessage().contains("key=items"));
      }

      @Test
      void doesNotThrowWhenAllRequiredKeysPresentWithCorrectTypes() {
        Spec spec = Spec.builder()
            .requiredString("name")
            .requiredNumber("count")
            .requiredBoolean("enabled")
            .requiredObject("nested")
            .requiredStringList("tags")
            .requiredNumberList("counts")
            .requiredBooleanList("flags")
            .requiredObjectList("items")
            .build();

        Config config = ConfigFactory.parseString(
            "name = test, count = 1, enabled = true, nested = {x = 1}, tags = [a, b], counts = [1, 2], flags = [true, false], items = [{a = 1}]");

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
      void doesNotThrowWhenOptionalObjectMissing() {
        Spec spec = Spec.builder()
            .optionalObject("nested")
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
      void doesNotThrowWhenOptionalObjectListMissing() {
        Spec spec = Spec.builder()
            .optionalObjectList("items")
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
      void throwsOnWrongTypeForOptionalObject() {
        Spec spec = Spec.builder()
            .optionalObject("nested")
            .build();

        Config config = ConfigFactory.parseString("nested = [1, 2, 3]");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected object)"));
        assertTrue(exception.getMessage().contains("key=nested"));
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
      void throwsOnWrongTypeForOptionalObjectList() {
        Spec spec = Spec.builder()
            .optionalObjectList("items")
            .build();

        Config config = ConfigFactory.parseString("items = 123");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Incorrect key type (expected list<object>)"));
        assertTrue(exception.getMessage().contains("key=items"));
      }

      @Test
      void doesNotThrowWhenAllOptionalKeysPresentWithCorrectTypes() {
        Spec spec = Spec.builder()
            .optionalString("name")
            .optionalNumber("count")
            .optionalBoolean("enabled")
            .optionalObject("nested")
            .optionalStringList("tags")
            .optionalNumberList("counts")
            .optionalBooleanList("flags")
            .optionalObjectList("items")
            .build();

        Config config = ConfigFactory.parseString(
            "name = test, count = 1, enabled = true, nested = {x = 1}, tags = [a, b], counts = [1, 2], flags = [true, false], items = [{a = 1}]");

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
      void throwsOnConstraintViolationForRequiredObject() {
        ObjectConstraint constraint = new TestObjectConstraint(value -> false, "has key a");
        Spec spec = Spec.builder()
            .requiredObject(constraint, "nested")
            .build();

        Config config = ConfigFactory.parseString("nested = { x = 1 }");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("has key a"));
        assertTrue(exception.getMessage().contains("key=nested"));
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
      void throwsOnConstraintViolationForRequiredObjectList() {
        ObjectListConstraint constraint = new TestObjectListConstraint(value -> false, "non-empty");
        Spec spec = Spec.builder()
            .requiredObjectList(constraint, "items")
            .build();

        Config config = ConfigFactory.parseString("items = []");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("non-empty"));
        assertTrue(exception.getMessage().contains("key=items"));
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
      void throwsOnConstraintViolationForOptionalObject() {
        ObjectConstraint constraint = new TestObjectConstraint(value -> false, "has key a");
        Spec spec = Spec.builder()
            .optionalObject(constraint, "nested")
            .build();

        Config config = ConfigFactory.parseString("nested = { x = 1 }");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("has key a"));
        assertTrue(exception.getMessage().contains("key=nested"));
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
      void throwsOnConstraintViolationForOptionalObjectList() {
        ObjectListConstraint constraint = new TestObjectListConstraint(value -> false, "non-empty");
        Spec spec = Spec.builder()
            .optionalObjectList(constraint, "items")
            .build();

        Config config = ConfigFactory.parseString("items = []");

        SpecException exception = spec.validate(config, ComponentType.ROOT, null, "test").get(0);

        assertTrue(exception.getMessage().contains("Invalid key value"));
        assertTrue(exception.getMessage().contains("non-empty"));
        assertTrue(exception.getMessage().contains("key=items"));
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
  class Collect {

    @Test
    void collectsFromOneLevelHierarchy() {
      Spec collected = Spec.collect(CollectBase.class);

      Config noBase = ConfigFactory.parseString("");
      Config complete = ConfigFactory.parseString("baseField = b");

      SpecException missingBase = collected.validate(noBase, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingBase.getMessage().contains("Missing required key"));
      assertTrue(missingBase.getMessage().contains("key=baseField"));
      assertTrue(collected.validate(complete, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void collectsFromTwoLevelHierarchy() {
      Spec collected = Spec.collect(CollectChild.class);

      Config noBase = ConfigFactory.parseString("childField = c");
      Config noChild = ConfigFactory.parseString("baseField = b");
      Config complete = ConfigFactory.parseString("baseField = b, childField = c");

      SpecException missingBase = collected.validate(noBase, ComponentType.ROOT, null, "test").get(0);
      SpecException missingChild = collected.validate(noChild, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingBase.getMessage().contains("Missing required key"));
      assertTrue(missingBase.getMessage().contains("key=baseField"));
      assertTrue(missingChild.getMessage().contains("Missing required key"));
      assertTrue(missingChild.getMessage().contains("key=childField"));
      assertTrue(collected.validate(complete, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void collectsFromThreeLevelHierarchy() {
      Spec collected = Spec.collect(CollectGrandchild.class);

      Config noBase = ConfigFactory.parseString("childField = c, grandchildField = g");
      Config noChild = ConfigFactory.parseString("baseField = b, grandchildField = g");
      Config noGrandchild = ConfigFactory.parseString("baseField = b, childField = c");
      Config complete = ConfigFactory.parseString("baseField = b, childField = c, grandchildField = g");

      SpecException missingBase = collected.validate(noBase, ComponentType.ROOT, null, "test").get(0);
      SpecException missingChild = collected.validate(noChild, ComponentType.ROOT, null, "test").get(0);
      SpecException missingGrandchild = collected.validate(noGrandchild, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingBase.getMessage().contains("Missing required key"));
      assertTrue(missingBase.getMessage().contains("key=baseField"));
      assertTrue(missingChild.getMessage().contains("Missing required key"));
      assertTrue(missingChild.getMessage().contains("key=childField"));
      assertTrue(missingGrandchild.getMessage().contains("Missing required key"));
      assertTrue(missingGrandchild.getMessage().contains("key=grandchildField"));
      assertTrue(collected.validate(complete, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void collectsFromHierarchyWithMissingSpec() {
      Spec collected = Spec.collect(CollectGrandchildMissing.class);

      Config noBase = ConfigFactory.parseString("grandchildField = g");
      Config noGrandchild = ConfigFactory.parseString("baseField = b");
      Config complete = ConfigFactory.parseString("baseField = b, grandchildField = g");

      SpecException missingBase = collected.validate(noBase, ComponentType.ROOT, null, "test").get(0);
      SpecException missingGrandchild = collected.validate(noGrandchild, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingBase.getMessage().contains("Missing required key"));
      assertTrue(missingBase.getMessage().contains("key=baseField"));
      assertTrue(missingGrandchild.getMessage().contains("Missing required key"));
      assertTrue(missingGrandchild.getMessage().contains("key=grandchildField"));
      assertTrue(collected.validate(complete, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void collectsFromHierarchyWithNoSpec() {
      Spec collected = Spec.collect(CollectNoSpecAnywhere.class);

      Config empty = ConfigFactory.parseString("");

      assertTrue(collected.validate(empty, ComponentType.ROOT, null, "test").isEmpty());
    }
  }

  @Nested
  class Union {

    @Test
    void requiredStringsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredString("nameA")
          .build();

      Spec specB = Spec.builder()
          .requiredString("nameB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("nameA = a");
      Config specBConfig = ConfigFactory.parseString("nameB = b");
      Config combinedConfig = ConfigFactory.parseString("nameA = a, nameB = b");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=nameB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=nameA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredNumbersFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredNumber("countA")
          .build();

      Spec specB = Spec.builder()
          .requiredNumber("countB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("countA = 1");
      Config specBConfig = ConfigFactory.parseString("countB = 2");
      Config combinedConfig = ConfigFactory.parseString("countA = 1, countB = 2");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=countB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=countA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredBooleansFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredBoolean("enabledA")
          .build();

      Spec specB = Spec.builder()
          .requiredBoolean("enabledB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("enabledA = true");
      Config specBConfig = ConfigFactory.parseString("enabledB = true");
      Config combinedConfig = ConfigFactory.parseString("enabledA = true, enabledB = true");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=enabledB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=enabledA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredObjectsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredObject("nestedA")
          .build();

      Spec specB = Spec.builder()
          .requiredObject("nestedB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("nestedA {}");
      Config specBConfig = ConfigFactory.parseString("nestedB {}");
      Config combinedConfig = ConfigFactory.parseString("nestedA {}, nestedB {}");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=nestedB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=nestedA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredStringListsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredStringList("tagsA")
          .build();

      Spec specB = Spec.builder()
          .requiredStringList("tagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("tagsA = [a]");
      Config specBConfig = ConfigFactory.parseString("tagsB = [b]");
      Config combinedConfig = ConfigFactory.parseString("tagsA = [a], tagsB = [b]");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=tagsB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=tagsA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredNumberListsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredNumberList("countsA")
          .build();

      Spec specB = Spec.builder()
          .requiredNumberList("countsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("countsA = [1]");
      Config specBConfig = ConfigFactory.parseString("countsB = [2]");
      Config combinedConfig = ConfigFactory.parseString("countsA = [1], countsB = [2]");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=countsB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=countsA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredBooleanListsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredBooleanList("flagsA")
          .build();

      Spec specB = Spec.builder()
          .requiredBooleanList("flagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("flagsA = [true]");
      Config specBConfig = ConfigFactory.parseString("flagsB = [true]");
      Config combinedConfig = ConfigFactory.parseString("flagsA = [true], flagsB = [true]");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=flagsB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=flagsA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void requiredObjectListsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .requiredObjectList("itemsA")
          .build();

      Spec specB = Spec.builder()
          .requiredObjectList("itemsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAConfig = ConfigFactory.parseString("itemsA = [{}]");
      Config specBConfig = ConfigFactory.parseString("itemsB = [{}]");
      Config combinedConfig = ConfigFactory.parseString("itemsA = [{}], itemsB = [{}]");

      SpecException missingB = union.validate(specAConfig, ComponentType.ROOT, null, "test").get(0);
      SpecException missingA = union.validate(specBConfig, ComponentType.ROOT, null, "test").get(0);

      assertTrue(missingB.getMessage().contains("Missing required key"));
      assertTrue(missingB.getMessage().contains("key=itemsB"));
      assertTrue(missingA.getMessage().contains("Missing required key"));
      assertTrue(missingA.getMessage().contains("key=itemsA"));
      assertTrue(union.validate(combinedConfig, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalStringsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalString("nameA")
          .build();

      Spec specB = Spec.builder()
          .optionalString("nameB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("nameA = a, nameB = b");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalNumbersFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalNumber("countA")
          .build();

      Spec specB = Spec.builder()
          .optionalNumber("countB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("countA = 1, countB = 2");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalBooleansFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalBoolean("enabledA")
          .build();

      Spec specB = Spec.builder()
          .optionalBoolean("enabledB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("enabledA = true, enabledB = true");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalObjectsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalObject("nestedA")
          .build();

      Spec specB = Spec.builder()
          .optionalObject("nestedB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("nestedA {}, nestedB {}");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalStringListsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalStringList("tagsA")
          .build();

      Spec specB = Spec.builder()
          .optionalStringList("tagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("tagsA = [a], tagsB = [b]");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalNumberListsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalNumberList("countsA")
          .build();

      Spec specB = Spec.builder()
          .optionalNumberList("countsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("countsA = [1], countsB = [2]");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalBooleanListsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalBooleanList("flagsA")
          .build();

      Spec specB = Spec.builder()
          .optionalBooleanList("flagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("flagsA = [true], flagsB = [false]");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void optionalObjectListsFromBothSpecsAreRecognized() {
      Spec specA = Spec.builder()
          .optionalObjectList("itemsA")
          .build();

      Spec specB = Spec.builder()
          .optionalObjectList("itemsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("itemsA = [{}], itemsB = [{}]");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void exactlyOneGroupsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .optionalString("a", "b")
          .exactlyOne("a", "b")
          .build();

      Spec specB = Spec.builder()
          .optionalString("c", "d")
          .exactlyOne("c", "d")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("a = 1, b = 2, c = 3");
      Config specBViolation = ConfigFactory.parseString("a = 1, c = 3, d = 4");
      Config valid = ConfigFactory.parseString("a = 1, c = 3");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key combination (expected exactly one of a, b)"));
      assertTrue(aViolation.getMessage().contains("count=2"));
      assertTrue(bViolation.getMessage().contains("Invalid key combination (expected exactly one of c, d)"));
      assertTrue(bViolation.getMessage().contains("count=2"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void atLeastOneGroupsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .optionalString("a", "b")
          .atLeastOne("a", "b")
          .build();

      Spec specB = Spec.builder()
          .optionalString("c", "d")
          .atLeastOne("c", "d")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("c = 3");
      Config specBViolation = ConfigFactory.parseString("a = 1");
      Config valid = ConfigFactory.parseString("a = 1, c = 3");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key combination (expected at least one of a, b)"));
      assertTrue(aViolation.getMessage().contains("count=0"));
      assertTrue(bViolation.getMessage().contains("Invalid key combination (expected at least one of c, d)"));
      assertTrue(bViolation.getMessage().contains("count=0"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void mutuallyInclusiveGroupsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .optionalString("a", "b")
          .mutuallyInclusive("a", "b")
          .build();

      Spec specB = Spec.builder()
          .optionalString("c", "d")
          .mutuallyInclusive("c", "d")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("a = 1, c = 3, d = 4");
      Config specBViolation = ConfigFactory.parseString("a = 1, b = 2, c = 3");
      Config valid = ConfigFactory.parseString("a = 1, b = 2, c = 3, d = 4");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key combination (expected all or none of a, b)"));
      assertTrue(aViolation.getMessage().contains("count=1"));
      assertTrue(bViolation.getMessage().contains("Invalid key combination (expected all or none of c, d)"));
      assertTrue(bViolation.getMessage().contains("count=1"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void mutuallyExclusiveGroupsFromBothSpecsAreEnforced() {
      Spec specA = Spec.builder()
          .optionalString("a", "b")
          .mutuallyExclusive("a", "b")
          .build();

      Spec specB = Spec.builder()
          .optionalString("c", "d")
          .mutuallyExclusive("c", "d")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("a = 1, b = 2, c = 3");
      Config specBViolation = ConfigFactory.parseString("a = 1, c = 3, d = 4");
      Config valid = ConfigFactory.parseString("a = 1, c = 3");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key combination (expected at most one of a, b)"));
      assertTrue(aViolation.getMessage().contains("count=2"));
      assertTrue(bViolation.getMessage().contains("Invalid key combination (expected at most one of c, d)"));
      assertTrue(bViolation.getMessage().contains("count=2"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void stringConstraintsFromBothSpecsAreEnforced() {
      StringConstraint rejectsFoo = new TestStringConstraint(v -> !v.equals("foo"), "not foo");

      Spec specA = Spec.builder()
          .requiredString(rejectsFoo, "nameA")
          .build();

      Spec specB = Spec.builder()
          .requiredString(rejectsFoo, "nameB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("nameA = foo, nameB = bar");
      Config specBViolation = ConfigFactory.parseString("nameA = bar, nameB = foo");
      Config valid = ConfigFactory.parseString("nameA = bar, nameB = bar");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("not foo"));
      assertTrue(aViolation.getMessage().contains("key=nameA"));
      assertTrue(aViolation.getMessage().contains("value=foo"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("not foo"));
      assertTrue(bViolation.getMessage().contains("key=nameB"));
      assertTrue(bViolation.getMessage().contains("value=foo"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void numberConstraintsFromBothSpecsAreEnforced() {
      NumberConstraint rejectsZero = new TestNumberConstraint(v -> v.intValue() != 0, "not zero");

      Spec specA = Spec.builder()
          .requiredNumber(rejectsZero, "countA")
          .build();

      Spec specB = Spec.builder()
          .requiredNumber(rejectsZero, "countB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("countA = 0, countB = 1");
      Config specBViolation = ConfigFactory.parseString("countA = 1, countB = 0");
      Config valid = ConfigFactory.parseString("countA = 1, countB = 1");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("not zero"));
      assertTrue(aViolation.getMessage().contains("key=countA"));
      assertTrue(aViolation.getMessage().contains("value=0"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("not zero"));
      assertTrue(bViolation.getMessage().contains("key=countB"));
      assertTrue(bViolation.getMessage().contains("value=0"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void booleanConstraintsFromBothSpecsAreEnforced() {
      BooleanConstraint rejectsTrue = new TestBooleanConstraint(v -> !v, "not true");

      Spec specA = Spec.builder()
          .requiredBoolean(rejectsTrue, "enabledA")
          .build();

      Spec specB = Spec.builder()
          .requiredBoolean(rejectsTrue, "enabledB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("enabledA = true, enabledB = false");
      Config specBViolation = ConfigFactory.parseString("enabledA = false, enabledB = true");
      Config valid = ConfigFactory.parseString("enabledA = false, enabledB = false");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("not true"));
      assertTrue(aViolation.getMessage().contains("key=enabledA"));
      assertTrue(aViolation.getMessage().contains("value=true"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("not true"));
      assertTrue(bViolation.getMessage().contains("key=enabledB"));
      assertTrue(bViolation.getMessage().contains("value=true"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void objectConstraintsFromBothSpecsAreEnforced() {
      ObjectConstraint rejectsHavingFoo = new TestObjectConstraint(c -> !c.hasPath("foo"), "no foo");

      Spec specA = Spec.builder()
          .requiredObject(rejectsHavingFoo, "nestedA")
          .build();

      Spec specB = Spec.builder()
          .requiredObject(rejectsHavingFoo, "nestedB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("nestedA { foo = 1 }, nestedB {}");
      Config specBViolation = ConfigFactory.parseString("nestedA {}, nestedB { foo = 1 }");
      Config valid = ConfigFactory.parseString("nestedA {}, nestedB {}");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("no foo"));
      assertTrue(aViolation.getMessage().contains("key=nestedA"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("no foo"));
      assertTrue(bViolation.getMessage().contains("key=nestedB"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void stringListConstraintsFromBothSpecsAreEnforced() {
      StringListConstraint rejectsHavingFoo = new TestStringListConstraint(list -> !list.contains("foo"), "no foo");

      Spec specA = Spec.builder()
          .requiredStringList(rejectsHavingFoo, "tagsA")
          .build();

      Spec specB = Spec.builder()
          .requiredStringList(rejectsHavingFoo, "tagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("tagsA = [foo], tagsB = [bar]");
      Config specBViolation = ConfigFactory.parseString("tagsA = [bar], tagsB = [foo]");
      Config valid = ConfigFactory.parseString("tagsA = [bar], tagsB = [bar]");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("no foo"));
      assertTrue(aViolation.getMessage().contains("key=tagsA"));
      assertTrue(aViolation.getMessage().contains("value=[foo]"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("no foo"));
      assertTrue(bViolation.getMessage().contains("key=tagsB"));
      assertTrue(bViolation.getMessage().contains("value=[foo]"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void numberListConstraintsFromBothSpecsAreEnforced() {
      NumberListConstraint rejectsHavingZero = new TestNumberListConstraint(
          list -> list.stream().noneMatch(n -> n.intValue() == 0), "no zero");

      Spec specA = Spec.builder()
          .requiredNumberList(rejectsHavingZero, "countsA")
          .build();

      Spec specB = Spec.builder()
          .requiredNumberList(rejectsHavingZero, "countsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("countsA = [0], countsB = [1]");
      Config specBViolation = ConfigFactory.parseString("countsA = [1], countsB = [0]");
      Config valid = ConfigFactory.parseString("countsA = [1], countsB = [1]");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("no zero"));
      assertTrue(aViolation.getMessage().contains("key=countsA"));
      assertTrue(aViolation.getMessage().contains("value=[0]"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("no zero"));
      assertTrue(bViolation.getMessage().contains("key=countsB"));
      assertTrue(bViolation.getMessage().contains("value=[0]"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void booleanListConstraintsFromBothSpecsAreEnforced() {
      BooleanListConstraint rejectsHavingTrue = new TestBooleanListConstraint(
          list -> !list.contains(true), "no true");

      Spec specA = Spec.builder()
          .requiredBooleanList(rejectsHavingTrue, "flagsA")
          .build();

      Spec specB = Spec.builder()
          .requiredBooleanList(rejectsHavingTrue, "flagsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("flagsA = [true], flagsB = [false]");
      Config specBViolation = ConfigFactory.parseString("flagsA = [false], flagsB = [true]");
      Config valid = ConfigFactory.parseString("flagsA = [false], flagsB = [false]");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("no true"));
      assertTrue(aViolation.getMessage().contains("key=flagsA"));
      assertTrue(aViolation.getMessage().contains("value=[true]"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("no true"));
      assertTrue(bViolation.getMessage().contains("key=flagsB"));
      assertTrue(bViolation.getMessage().contains("value=[true]"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void objectListConstraintsFromBothSpecsAreEnforced() {
      ObjectListConstraint rejectsEmpty = new TestObjectListConstraint(list -> !list.isEmpty(), "non-empty");

      Spec specA = Spec.builder()
          .requiredObjectList(rejectsEmpty, "itemsA")
          .build();

      Spec specB = Spec.builder()
          .requiredObjectList(rejectsEmpty, "itemsB")
          .build();

      Spec union = Spec.union(specA, specB);

      Config specAViolation = ConfigFactory.parseString("itemsA = [], itemsB = [{}]");
      Config specBViolation = ConfigFactory.parseString("itemsA = [{}], itemsB = []");
      Config valid = ConfigFactory.parseString("itemsA = [{}], itemsB = [{}]");

      SpecException aViolation = union.validate(specAViolation, ComponentType.ROOT, null, "test").get(0);
      SpecException bViolation = union.validate(specBViolation, ComponentType.ROOT, null, "test").get(0);

      assertTrue(aViolation.getMessage().contains("Invalid key value"));
      assertTrue(aViolation.getMessage().contains("key=itemsA"));
      assertTrue(bViolation.getMessage().contains("Invalid key value"));
      assertTrue(bViolation.getMessage().contains("key=itemsB"));
      assertTrue(union.validate(valid, ComponentType.ROOT, null, "test").isEmpty());
    }

    @Test
    void throwsOnUnknownKeyNotInEitherSpec() {
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
    void doesNotThrowOnEmptyConfigForEmptyUnion() {
      Spec specA = Spec.builder().build();
      Spec specB = Spec.builder().build();

      Spec union = Spec.union(specA, specB);

      Config config = ConfigFactory.parseString("");

      assertTrue(union.validate(config, ComponentType.ROOT, null, "test").isEmpty());
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

  private static final class TestObjectConstraint implements ObjectConstraint {

    private final Predicate<Config> predicate;
    private final String description;

    private TestObjectConstraint(Predicate<Config> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(Config value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static final class TestObjectListConstraint implements ObjectListConstraint {

    private final Predicate<List<? extends Config>> predicate;
    private final String description;

    private TestObjectListConstraint(Predicate<List<? extends Config>> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    @Override
    public boolean test(List<? extends Config> value) {
      return predicate.test(value);
    }

    @Override
    public String describe() {
      return description;
    }
  }

  private static class CollectBase {
    @SuppressWarnings("unused")
    static final Spec SPEC = Spec.builder().requiredString("baseField").build();
  }

  private static class CollectChild extends CollectBase {
    @SuppressWarnings("unused")
    static final Spec SPEC = Spec.builder().requiredString("childField").build();
  }

  private static class CollectGrandchild extends CollectChild {
    @SuppressWarnings("unused")
    static final Spec SPEC = Spec.builder().requiredString("grandchildField").build();
  }

  private static class CollectChildMissing extends CollectBase {
  }

  private static class CollectGrandchildMissing extends CollectChildMissing {
    @SuppressWarnings("unused")
    static final Spec SPEC = Spec.builder().requiredString("grandchildField").build();
  }

  private static class CollectNoSpecAnywhere {
  }
}
