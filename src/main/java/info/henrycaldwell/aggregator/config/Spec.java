package info.henrycaldwell.aggregator.config;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;

import info.henrycaldwell.aggregator.error.SpecException;

/**
 * Class for validating HOCON configuration blocks.
 * 
 * This class records required and optional keys by primitive type and list type
 * and validates configuration blocks for unknown keys, missing required keys,
 * and type mismatches.
 */
public final class Spec {

  private final Set<String> requiredStrings = new LinkedHashSet<>();
  private final Set<String> optionalStrings = new LinkedHashSet<>();
  private final Set<String> requiredNumbers = new LinkedHashSet<>();
  private final Set<String> optionalNumbers = new LinkedHashSet<>();
  private final Set<String> requiredBooleans = new LinkedHashSet<>();
  private final Set<String> optionalBooleans = new LinkedHashSet<>();
  private final Set<String> requiredStringLists = new LinkedHashSet<>();
  private final Set<String> optionalStringLists = new LinkedHashSet<>();
  private final Set<String> requiredNumberLists = new LinkedHashSet<>();
  private final Set<String> optionalNumberLists = new LinkedHashSet<>();
  private final Set<String> requiredBooleanLists = new LinkedHashSet<>();
  private final Set<String> optionalBooleanLists = new LinkedHashSet<>();

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
      composite.requiredStringLists.addAll(spec.requiredStringLists);
      composite.optionalStringLists.addAll(spec.optionalStringLists);
      composite.requiredNumberLists.addAll(spec.requiredNumberLists);
      composite.optionalNumberLists.addAll(spec.optionalNumberLists);
      composite.requiredBooleanLists.addAll(spec.requiredBooleanLists);
      composite.optionalBooleanLists.addAll(spec.optionalBooleanLists);
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
   * Validates a configuration block against this spec.
   *
   * @param config A {@link Config} representing the block to validate.
   * @param name   A string representing a display name.
   * @throws SpecException if validation fails at any step.
   */
  public void validate(Config config, String name) {
    Set<String> legal = new LinkedHashSet<>();
    Set<String> required = new LinkedHashSet<>();

    legal.addAll(requiredStrings);
    legal.addAll(optionalStrings);
    legal.addAll(requiredNumbers);
    legal.addAll(optionalNumbers);
    legal.addAll(requiredBooleans);
    legal.addAll(optionalBooleans);
    legal.addAll(requiredStringLists);
    legal.addAll(optionalStringLists);
    legal.addAll(requiredNumberLists);
    legal.addAll(optionalNumberLists);
    legal.addAll(requiredBooleanLists);
    legal.addAll(optionalBooleanLists);

    required.addAll(requiredStrings);
    required.addAll(requiredNumbers);
    required.addAll(requiredBooleans);
    required.addAll(requiredStringLists);
    required.addAll(requiredNumberLists);
    required.addAll(requiredBooleanLists);

    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String key = entry.getKey();

      if (!legal.contains(key)) {
        throw new SpecException(name, "Unknown configuration key", Map.of("key", key));
      }
    }

    for (String key : required) {
      if (!config.hasPath(key)) {
        throw new SpecException(name, "Missing required key", Map.of("key", key));
      }
    }

    for (String key : requiredStrings) {
      try {
        config.getString(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected string)", Map.of("key", key), e);
      }
    }

    for (String key : requiredNumbers) {
      try {
        config.getNumber(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected number)", Map.of("key", key), e);
      }
    }

    for (String key : requiredBooleans) {
      try {
        config.getBoolean(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected boolean)", Map.of("key", key), e);
      }
    }

    for (String key : requiredStringLists) {
      try {
        config.getStringList(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected list<string>)", Map.of("key", key), e);
      }
    }

    for (String key : requiredNumberLists) {
      try {
        config.getNumberList(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected list<number>)", Map.of("key", key), e);
      }
    }

    for (String key : requiredBooleanLists) {
      try {
        config.getBooleanList(key);
      } catch (ConfigException.WrongType e) {
        throw new SpecException(name, "Incorrect key type (expected list<boolean>)", Map.of("key", key), e);
      }
    }

    for (String key : optionalStrings) {
      if (config.hasPath(key)) {
        try {
          config.getString(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected string)", Map.of("key", key), e);
        }
      }
    }

    for (String key : optionalNumbers) {
      if (config.hasPath(key)) {
        try {
          config.getNumber(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected number)", Map.of("key", key), e);
        }
      }
    }

    for (String key : optionalBooleans) {
      if (config.hasPath(key)) {
        try {
          config.getBoolean(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected boolean)", Map.of("key", key), e);
        }
      }
    }

    for (String key : optionalStringLists) {
      if (config.hasPath(key)) {
        try {
          config.getStringList(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected list<string>)", Map.of("key", key), e);
        }
      }
    }

    for (String key : optionalNumberLists) {
      if (config.hasPath(key)) {
        try {
          config.getNumberList(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected list<number>)", Map.of("key", key), e);
        }
      }
    }

    for (String key : optionalBooleanLists) {
      if (config.hasPath(key)) {
        try {
          config.getBooleanList(key);
        } catch (ConfigException.WrongType e) {
          throw new SpecException(name, "Incorrect key type (expected list<boolean>)", Map.of("key", key), e);
        }
      }
    }
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
     * Builds the configured spec instance.
     *
     * @return A {@link Spec} containing the accumulated required and optional keys.
     */
    public Spec build() {
      return spec;
    }
  }
}
