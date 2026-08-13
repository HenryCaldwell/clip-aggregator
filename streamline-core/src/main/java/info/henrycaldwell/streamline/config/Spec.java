package info.henrycaldwell.streamline.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import info.henrycaldwell.streamline.error.ComponentType;
import info.henrycaldwell.streamline.error.SpecException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for validating HOCON configuration blocks.
 * 
 * This class records required and optional keys by primitive type and list type
 * and validates configuration blocks for unknown keys, missing required keys,
 * type mismatches, and value constraints.
 */
public final class Spec {

  private final Set<String> requiredStrings = new LinkedHashSet<>();
  private final Set<String> optionalStrings = new LinkedHashSet<>();
  private final Set<String> requiredNumbers = new LinkedHashSet<>();
  private final Set<String> optionalNumbers = new LinkedHashSet<>();
  private final Set<String> requiredBooleans = new LinkedHashSet<>();
  private final Set<String> optionalBooleans = new LinkedHashSet<>();
  private final Set<String> requiredObjects = new LinkedHashSet<>();
  private final Set<String> optionalObjects = new LinkedHashSet<>();
  private final Set<String> requiredStringLists = new LinkedHashSet<>();
  private final Set<String> optionalStringLists = new LinkedHashSet<>();
  private final Set<String> requiredNumberLists = new LinkedHashSet<>();
  private final Set<String> optionalNumberLists = new LinkedHashSet<>();
  private final Set<String> requiredBooleanLists = new LinkedHashSet<>();
  private final Set<String> optionalBooleanLists = new LinkedHashSet<>();
  private final Set<String> requiredObjectLists = new LinkedHashSet<>();
  private final Set<String> optionalObjectLists = new LinkedHashSet<>();

  private final List<List<String>> exactlyOneGroups = new ArrayList<>();
  private final List<List<String>> atLeastOneGroups = new ArrayList<>();
  private final List<List<String>> mutuallyInclusiveGroups = new ArrayList<>();
  private final List<List<String>> mutuallyExclusiveGroups = new ArrayList<>();

  private final Map<String, StringConstraint> stringConstraints = new LinkedHashMap<>();
  private final Map<String, NumberConstraint> numberConstraints = new LinkedHashMap<>();
  private final Map<String, BooleanConstraint> booleanConstraints = new LinkedHashMap<>();
  private final Map<String, ObjectConstraint> objectConstraints = new LinkedHashMap<>();
  private final Map<String, StringListConstraint> stringListConstraints = new LinkedHashMap<>();
  private final Map<String, NumberListConstraint> numberListConstraints = new LinkedHashMap<>();
  private final Map<String, BooleanListConstraint> booleanListConstraints = new LinkedHashMap<>();
  private final Map<String, ObjectListConstraint> objectListConstraints = new LinkedHashMap<>();

  /**
   * Creates a new builder for constructing a spec.
   *
   * @return A {@link SpecBuilder} for defining required and optional keys.
   */
  public static SpecBuilder builder() {
    return new SpecBuilder();
  }

  /**
   * Merges required and optional keys across the provided specs.
   * 
   * @param specs An array of {@link Spec} values representing the specs to merge.
   * @return A {@link Spec} representing the combined set of keys.
   */
  public static Spec union(Spec... specs) {
    Spec composite = new Spec();

    for (Spec spec : specs) {
      composite.requiredStrings.addAll(spec.requiredStrings);
      composite.optionalStrings.addAll(spec.optionalStrings);
      composite.requiredNumbers.addAll(spec.requiredNumbers);
      composite.optionalNumbers.addAll(spec.optionalNumbers);
      composite.requiredBooleans.addAll(spec.requiredBooleans);
      composite.optionalBooleans.addAll(spec.optionalBooleans);
      composite.requiredObjects.addAll(spec.requiredObjects);
      composite.optionalObjects.addAll(spec.optionalObjects);
      composite.requiredStringLists.addAll(spec.requiredStringLists);
      composite.optionalStringLists.addAll(spec.optionalStringLists);
      composite.requiredNumberLists.addAll(spec.requiredNumberLists);
      composite.optionalNumberLists.addAll(spec.optionalNumberLists);
      composite.requiredBooleanLists.addAll(spec.requiredBooleanLists);
      composite.optionalBooleanLists.addAll(spec.optionalBooleanLists);
      composite.requiredObjectLists.addAll(spec.requiredObjectLists);
      composite.optionalObjectLists.addAll(spec.optionalObjectLists);

      composite.exactlyOneGroups.addAll(spec.exactlyOneGroups);
      composite.atLeastOneGroups.addAll(spec.atLeastOneGroups);
      composite.mutuallyInclusiveGroups.addAll(spec.mutuallyInclusiveGroups);
      composite.mutuallyExclusiveGroups.addAll(spec.mutuallyExclusiveGroups);

      composite.stringConstraints.putAll(spec.stringConstraints);
      composite.numberConstraints.putAll(spec.numberConstraints);
      composite.booleanConstraints.putAll(spec.booleanConstraints);
      composite.objectConstraints.putAll(spec.objectConstraints);
      composite.stringListConstraints.putAll(spec.stringListConstraints);
      composite.numberListConstraints.putAll(spec.numberListConstraints);
      composite.booleanListConstraints.putAll(spec.booleanListConstraints);
      composite.objectListConstraints.putAll(spec.objectListConstraints);
    }

    return composite;
  }

  /**
   * Adds a single required string key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredString(String param) {
    requiredStrings.add(param);
  }

  /**
   * Adds a single optional string key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalString(String param) {
    optionalStrings.add(param);
  }

  /**
   * Adds a single required number key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredNumber(String param) {
    requiredNumbers.add(param);
  }

  /**
   * Adds a single optional number key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalNumber(String param) {
    optionalNumbers.add(param);
  }

  /**
   * Adds a single required boolean key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredBoolean(String param) {
    requiredBooleans.add(param);
  }

  /**
   * Adds a single optional boolean key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalBoolean(String param) {
    optionalBooleans.add(param);
  }

  /**
   * Adds a single required object key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredObject(String param) {
    requiredObjects.add(param);
  }

  /**
   * Adds a single optional object key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalObject(String param) {
    optionalObjects.add(param);
  }

  /**
   * Adds a single required string list key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredStringList(String param) {
    requiredStringLists.add(param);
  }

  /**
   * Adds a single optional string list key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalStringList(String param) {
    optionalStringLists.add(param);
  }

  /**
   * Adds a single required number list key to this spec.
   * 
   * @param param A string representing the key name.
   */
  private void addRequiredNumberList(String param) {
    requiredNumberLists.add(param);
  }

  /**
   * Adds a single optional number list key to this spec.
   * 
   * @param param A string representing the key name.
   */
  private void addOptionalNumberList(String param) {
    optionalNumberLists.add(param);
  }

  /**
   * Adds a single required boolean list key to this spec.
   * 
   * @param param A string representing the key name.
   */
  private void addRequiredBooleanList(String param) {
    requiredBooleanLists.add(param);
  }

  /**
   * Adds a single optional boolean list key to this spec.
   * 
   * @param param A string representing the key name.
   */
  private void addOptionalBooleanList(String param) {
    optionalBooleanLists.add(param);
  }

  /**
   * Adds a single required object list key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addRequiredObjectList(String param) {
    requiredObjectLists.add(param);
  }

  /**
   * Adds a single optional object list key to this spec.
   *
   * @param param A string representing the key name.
   */
  private void addOptionalObjectList(String param) {
    optionalObjectLists.add(param);
  }

  /**
   * Adds an exactly-one key group to this spec.
   *
   * @param keys A {@link List} of strings representing the group keys.
   */
  private void addExactlyOneGroup(List<String> keys) {
    exactlyOneGroups.add(keys);
  }

  /**
   * Adds an at-least-one key group to this spec.
   *
   * @param keys A {@link List} of strings representing the group keys.
   */
  private void addAtLeastOneGroup(List<String> keys) {
    atLeastOneGroups.add(keys);
  }

  /**
   * Adds a mutually inclusive key group to this spec.
   *
   * @param keys A {@link List} of strings representing the group keys.
   */
  private void addMutuallyInclusiveGroup(List<String> keys) {
    mutuallyInclusiveGroups.add(keys);
  }

  /**
   * Adds a mutually exclusive key group to this spec.
   *
   * @param keys A {@link List} of strings representing the group keys.
   */
  private void addMutuallyExclusiveGroup(List<String> keys) {
    mutuallyExclusiveGroups.add(keys);
  }

  /**
   * Attaches a value constraint to a string key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link StringConstraint} representing the value
   *                   constraint.
   */
  private void constrainString(String param, StringConstraint constraint) {
    stringConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to a number key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link NumberConstraint} representing the value
   *                   constraint.
   */
  private void constrainNumber(String param, NumberConstraint constraint) {
    numberConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to a boolean key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link BooleanConstraint} representing the value
   *                   constraint.
   */
  private void constrainBoolean(String param, BooleanConstraint constraint) {
    booleanConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to an object key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint An {@link ObjectConstraint} representing the value
   *                   constraint.
   */
  private void constrainObject(String param, ObjectConstraint constraint) {
    objectConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to a string list key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link StringListConstraint} representing the value
   *                   constraint.
   */
  private void constrainStringList(String param, StringListConstraint constraint) {
    stringListConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to a number list key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link NumberListConstraint} representing the value
   *                   constraint.
   */
  private void constrainNumberList(String param, NumberListConstraint constraint) {
    numberListConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to a boolean list key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint A {@link BooleanListConstraint} representing the value
   *                   constraint.
   */
  private void constrainBooleanList(String param, BooleanListConstraint constraint) {
    booleanListConstraints.put(param, constraint);
  }

  /**
   * Attaches a value constraint to an object list key in this spec.
   *
   * @param param      A string representing the key name.
   * @param constraint An {@link ObjectListConstraint} representing the value
   *                   constraint.
   */
  private void constrainObjectList(String param, ObjectListConstraint constraint) {
    objectListConstraints.put(param, constraint);
  }

  /**
   * Validates a configuration block against this spec.
   *
   * @param config A {@link Config} representing the block to validate.
   * @param type   A {@link ComponentType} representing the component type.
   * @param parent A string representing the parent component name, or
   *               {@code null}.
   * @param name   A string representing a display name.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public List<SpecException> validate(Config config, ComponentType type, String parent, String name) {
    return validate(config, type, parent, name, -1);
  }

  /**
   * Validates a configuration block against this spec with a list index.
   *
   * @param config A {@link Config} representing the block to validate.
   * @param type   A {@link ComponentType} representing the component type.
   * @param parent A string representing the parent component name, or
   *               {@code null}.
   * @param name   A string representing a display name.
   * @param index  An integer representing the list index of the block, or
   *               {@code -1} when the block is not part of a list.
   * @return A {@link List} of {@link SpecException} representing the accumulated
   *         validation exceptions, or an empty list if validation passes.
   */
  public List<SpecException> validate(Config config, ComponentType type, String parent, String name, int index) {
    List<SpecException> exceptions = new ArrayList<>();

    Set<String> legal = new LinkedHashSet<>();
    Set<String> required = new LinkedHashSet<>();
    Set<String> failed = new HashSet<>();

    legal.addAll(requiredStrings);
    legal.addAll(optionalStrings);
    legal.addAll(requiredNumbers);
    legal.addAll(optionalNumbers);
    legal.addAll(requiredBooleans);
    legal.addAll(optionalBooleans);
    legal.addAll(requiredObjects);
    legal.addAll(optionalObjects);
    legal.addAll(requiredStringLists);
    legal.addAll(optionalStringLists);
    legal.addAll(requiredNumberLists);
    legal.addAll(optionalNumberLists);
    legal.addAll(requiredBooleanLists);
    legal.addAll(optionalBooleanLists);
    legal.addAll(requiredObjectLists);
    legal.addAll(optionalObjectLists);

    required.addAll(requiredStrings);
    required.addAll(requiredNumbers);
    required.addAll(requiredBooleans);
    required.addAll(requiredObjects);
    required.addAll(requiredStringLists);
    required.addAll(requiredNumberLists);
    required.addAll(requiredBooleanLists);
    required.addAll(requiredObjectLists);

    for (String key : config.root().keySet()) {
      if (!legal.contains(key)) {
        exceptions.add(new SpecException(type, parent, name, "Unknown configuration key",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key)));
      }
    }

    for (String key : required) {
      if (!config.hasPath(key)) {
        exceptions.add(new SpecException(type, parent, name, "Missing required key",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key)));
        failed.add(key);
      }
    }

    for (String key : requiredStrings) {
      if (failed.contains(key)) {
        continue;
      }

      String value;

      try {
        value = config.getString(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected string)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      if (value.isBlank()) {
        exceptions.add(new SpecException(type, parent, name, "Missing required key",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key)));
        continue;
      }

      StringConstraint constraint = stringConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredNumbers) {
      if (failed.contains(key)) {
        continue;
      }

      Number value;

      try {
        value = config.getNumber(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected number)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      NumberConstraint constraint = numberConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredBooleans) {
      if (failed.contains(key)) {
        continue;
      }

      Boolean value;

      try {
        value = config.getBoolean(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected boolean)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      BooleanConstraint constraint = booleanConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredObjects) {
      if (failed.contains(key)) {
        continue;
      }

      Config value;

      try {
        value = config.getConfig(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected object)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      ObjectConstraint constraint = objectConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key)
                : MapUtils.ofNullable("key", key)));
      }
    }

    for (String key : requiredStringLists) {
      if (failed.contains(key)) {
        continue;
      }

      List<String> value;

      try {
        value = config.getStringList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<string>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      StringListConstraint constraint = stringListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredNumberLists) {
      if (failed.contains(key)) {
        continue;
      }

      List<Number> value;

      try {
        value = config.getNumberList(key).stream().map(n -> (Number) n).toList();
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<number>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      NumberListConstraint constraint = numberListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredBooleanLists) {
      if (failed.contains(key)) {
        continue;
      }

      List<Boolean> value;

      try {
        value = config.getBooleanList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<boolean>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      BooleanListConstraint constraint = booleanListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : requiredObjectLists) {
      if (failed.contains(key)) {
        continue;
      }

      List<? extends Config> value;

      try {
        value = config.getConfigList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<object>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      ObjectListConstraint constraint = objectListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key)
                : MapUtils.ofNullable("key", key)));
      }
    }

    for (String key : optionalStrings) {
      if (!config.hasPath(key)) {
        continue;
      }

      String value;

      try {
        value = config.getString(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected string)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      StringConstraint constraint = stringConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalNumbers) {
      if (!config.hasPath(key)) {
        continue;
      }

      Number value;

      try {
        value = config.getNumber(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected number)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      NumberConstraint constraint = numberConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalBooleans) {
      if (!config.hasPath(key)) {
        continue;
      }

      Boolean value;

      try {
        value = config.getBoolean(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected boolean)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      BooleanConstraint constraint = booleanConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalObjects) {
      if (!config.hasPath(key)) {
        continue;
      }

      Config value;

      try {
        value = config.getConfig(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected object)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      ObjectConstraint constraint = objectConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key)
                : MapUtils.ofNullable("key", key)));
      }
    }

    for (String key : optionalStringLists) {
      if (!config.hasPath(key)) {
        continue;
      }

      List<String> value;

      try {
        value = config.getStringList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<string>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      StringListConstraint constraint = stringListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalNumberLists) {
      if (!config.hasPath(key)) {
        continue;
      }

      List<Number> value;

      try {
        value = config.getNumberList(key).stream().map(n -> (Number) n).toList();
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<number>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      NumberListConstraint constraint = numberListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalBooleanLists) {
      if (!config.hasPath(key)) {
        continue;
      }

      List<Boolean> value;

      try {
        value = config.getBooleanList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<boolean>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      BooleanListConstraint constraint = booleanListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key, "value", value)
                : MapUtils.ofNullable("key", key, "value", value)));
      }
    }

    for (String key : optionalObjectLists) {
      if (!config.hasPath(key)) {
        continue;
      }

      List<? extends Config> value;

      try {
        value = config.getConfigList(key);
      } catch (ConfigException.WrongType e) {
        exceptions.add(new SpecException(type, parent, name, "Incorrect key type (expected list<object>)",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key) : MapUtils.ofNullable("key", key), e));
        continue;
      }

      ObjectListConstraint constraint = objectListConstraints.get(key);

      if (constraint != null && !constraint.test(value)) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key value (expected " + key + " to be " + constraint.describe() + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "key", key)
                : MapUtils.ofNullable("key", key)));
      }
    }

    for (List<String> keys : exactlyOneGroups) {
      long count = keys.stream().filter(config::hasPath).count();

      if (count != 1) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key combination (expected exactly one of " + String.join(", ", keys) + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "keys", keys, "count", count)
                : MapUtils.ofNullable("keys", keys, "count", count)));
      }
    }

    for (List<String> keys : atLeastOneGroups) {
      long count = keys.stream().filter(config::hasPath).count();

      if (count < 1) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key combination (expected at least one of " + String.join(", ", keys) + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "keys", keys, "count", count)
                : MapUtils.ofNullable("keys", keys, "count", count)));
      }
    }

    for (List<String> keys : mutuallyInclusiveGroups) {
      long count = keys.stream().filter(config::hasPath).count();

      if (count != 0 && count != keys.size()) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key combination (expected all or none of " + String.join(", ", keys) + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "keys", keys, "count", count)
                : MapUtils.ofNullable("keys", keys, "count", count)));
      }
    }

    for (List<String> keys : mutuallyExclusiveGroups) {
      long count = keys.stream().filter(config::hasPath).count();

      if (count > 1) {
        exceptions.add(new SpecException(type, parent, name,
            "Invalid key combination (expected at most one of " + String.join(", ", keys) + ")",
            index >= 0 ? MapUtils.ofNullable("index", index, "keys", keys, "count", count)
                : MapUtils.ofNullable("keys", keys, "count", count)));
      }
    }

    return exceptions;
  }

  /**
   * Class for building a spec with required and optional keys.
   * 
   * This class collects desired keys by primitive type and produces a configured
   * {@link Spec} instance.
   */
  public static final class SpecBuilder {

    private final Spec spec = new Spec();

    /**
     * Adds one or more required string keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredString(String... params) {
      for (String param : params) {
        spec.addRequiredString(param);
      }

      return this;
    }

    /**
     * Adds one or more required string keys with a value constraint to the spec.
     *
     * @param constraint A {@link StringConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredString(StringConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredString(param);
        spec.constrainString(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional string keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalString(String... params) {
      for (String param : params) {
        spec.addOptionalString(param);
      }

      return this;
    }

    /**
     * Adds one or more optional string keys with a value constraint to the spec.
     *
     * @param constraint A {@link StringConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalString(StringConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalString(param);
        spec.constrainString(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required number keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredNumber(String... params) {
      for (String param : params) {
        spec.addRequiredNumber(param);
      }

      return this;
    }

    /**
     * Adds one or more required number keys with a value constraint to the spec.
     *
     * @param constraint A {@link NumberConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredNumber(NumberConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredNumber(param);
        spec.constrainNumber(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional number keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalNumber(String... params) {
      for (String param : params) {
        spec.addOptionalNumber(param);
      }

      return this;
    }

    /**
     * Adds one or more optional number keys with a value constraint to the spec.
     *
     * @param constraint A {@link NumberConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalNumber(NumberConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalNumber(param);
        spec.constrainNumber(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required boolean keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredBoolean(String... params) {
      for (String param : params) {
        spec.addRequiredBoolean(param);
      }

      return this;
    }

    /**
     * Adds one or more required boolean keys with a value constraint to the spec.
     *
     * @param constraint A {@link BooleanConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredBoolean(BooleanConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredBoolean(param);
        spec.constrainBoolean(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional boolean keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalBoolean(String... params) {
      for (String param : params) {
        spec.addOptionalBoolean(param);
      }

      return this;
    }

    /**
     * Adds one or more optional boolean keys with a value constraint to the spec.
     *
     * @param constraint A {@link BooleanConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalBoolean(BooleanConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalBoolean(param);
        spec.constrainBoolean(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required object keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredObject(String... params) {
      for (String param : params) {
        spec.addRequiredObject(param);
      }

      return this;
    }

    /**
     * Adds one or more required object keys with a value constraint to the spec.
     *
     * @param constraint An {@link ObjectConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredObject(ObjectConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredObject(param);
        spec.constrainObject(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional object keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalObject(String... params) {
      for (String param : params) {
        spec.addOptionalObject(param);
      }

      return this;
    }

    /**
     * Adds one or more optional object keys with a value constraint to the spec.
     *
     * @param constraint An {@link ObjectConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalObject(ObjectConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalObject(param);
        spec.constrainObject(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required string list keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredStringList(String... params) {
      for (String param : params) {
        spec.addRequiredStringList(param);
      }

      return this;
    }

    /**
     * Adds one or more required string list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link StringListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredStringList(StringListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredStringList(param);
        spec.constrainStringList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional string list keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalStringList(String... params) {
      for (String param : params) {
        spec.addOptionalStringList(param);
      }

      return this;
    }

    /**
     * Adds one or more optional string list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link StringListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalStringList(StringListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalStringList(param);
        spec.constrainStringList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required number list keys to the spec.
     * 
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredNumberList(String... params) {
      for (String param : params) {
        spec.addRequiredNumberList(param);
      }

      return this;
    }

    /**
     * Adds one or more required number list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link NumberListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredNumberList(NumberListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredNumberList(param);
        spec.constrainNumberList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional number list keys to the spec.
     * 
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalNumberList(String... params) {
      for (String param : params) {
        spec.addOptionalNumberList(param);
      }

      return this;
    }

    /**
     * Adds one or more optional number list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link NumberListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalNumberList(NumberListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalNumberList(param);
        spec.constrainNumberList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required boolean list keys to the spec.
     * 
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredBooleanList(String... params) {
      for (String param : params) {
        spec.addRequiredBooleanList(param);
      }

      return this;
    }

    /**
     * Adds one or more required boolean list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link BooleanListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredBooleanList(BooleanListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredBooleanList(param);
        spec.constrainBooleanList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional boolean list keys to the spec.
     * 
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalBooleanList(String... params) {
      for (String param : params) {
        spec.addOptionalBooleanList(param);
      }

      return this;
    }

    /**
     * Adds one or more optional boolean list keys with a value constraint to the
     * spec.
     *
     * @param constraint A {@link BooleanListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalBooleanList(BooleanListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalBooleanList(param);
        spec.constrainBooleanList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more required object list keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredObjectList(String... params) {
      for (String param : params) {
        spec.addRequiredObjectList(param);
      }

      return this;
    }

    /**
     * Adds one or more required object list keys with a value constraint to the
     * spec.
     *
     * @param constraint An {@link ObjectListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder requiredObjectList(ObjectListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addRequiredObjectList(param);
        spec.constrainObjectList(param, constraint);
      }

      return this;
    }

    /**
     * Adds one or more optional object list keys to the spec.
     *
     * @param params An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalObjectList(String... params) {
      for (String param : params) {
        spec.addOptionalObjectList(param);
      }

      return this;
    }

    /**
     * Adds one or more optional object list keys with a value constraint to the
     * spec.
     *
     * @param constraint An {@link ObjectListConstraint} representing the value
     *                   constraint.
     * @param params     An array of strings representing key names.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder optionalObjectList(ObjectListConstraint constraint, String... params) {
      for (String param : params) {
        spec.addOptionalObjectList(param);
        spec.constrainObjectList(param, constraint);
      }

      return this;
    }

    /**
     * Adds an exactly-one key group to the spec.
     *
     * @param keys An array of strings representing the group keys.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder exactlyOne(String... keys) {
      spec.addExactlyOneGroup(List.of(keys));
      return this;
    }

    /**
     * Adds an at-least-one key group to the spec.
     *
     * @param keys An array of strings representing the group keys.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder atLeastOne(String... keys) {
      spec.addAtLeastOneGroup(List.of(keys));
      return this;
    }

    /**
     * Adds a mutually inclusive key group to the spec.
     *
     * @param keys An array of strings representing the group keys.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder mutuallyInclusive(String... keys) {
      spec.addMutuallyInclusiveGroup(List.of(keys));
      return this;
    }

    /**
     * Adds a mutually exclusive key group to the spec.
     *
     * @param keys An array of strings representing the group keys.
     * @return A {@link SpecBuilder} for chaining additional keys.
     */
    public SpecBuilder mutuallyExclusive(String... keys) {
      spec.addMutuallyExclusiveGroup(List.of(keys));
      return this;
    }

    /**
     * Builds the configured spec instance.
     *
     * @return A {@link Spec} containing the accumulated required and optional keys.
     */
    public Spec build() {
      return spec;
    }
  }
}
