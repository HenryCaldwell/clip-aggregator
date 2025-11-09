package info.henrycaldwell.aggregator.config;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValue;

/**
 * Class for validating HOCON configuration blocks against type requirements.
 * 
 * This class defines required and optional keys by primitive type (string,
 * number, boolean) and validates presence, unknown keys, and type access.
 */
public final class Spec {

  private final Set<String> requiredStrings = new LinkedHashSet<>();
  private final Set<String> optionalStrings = new LinkedHashSet<>();
  private final Set<String> requiredNumbers = new LinkedHashSet<>();
  private final Set<String> optionalNumbers = new LinkedHashSet<>();
  private final Set<String> requiredBooleans = new LinkedHashSet<>();
  private final Set<String> optionalBooleans = new LinkedHashSet<>();

  /**
   * Creates a new builder for constructing a {@link Spec}.
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
   * Validates a configuration block against the spec.
   *
   * @param config A {@link Config} representing the block to validate.
   * @param name   A string representing a display name for error messages.
   * @throws IllegalArgumentException if validation fails at any step.
   */
  public void validate(Config config, String name) {
    Set<String> legal = new LinkedHashSet<>();

    legal.addAll(requiredStrings);
    legal.addAll(optionalStrings);
    legal.addAll(requiredNumbers);
    legal.addAll(optionalNumbers);
    legal.addAll(requiredBooleans);
    legal.addAll(optionalBooleans);

    for (Map.Entry<String, ConfigValue> entry : config.entrySet()) {
      String key = entry.getKey();

      if (!legal.contains(key)) {
        throw new IllegalArgumentException("Unknown key " + key + " (" + name + ")");
      }
    }

    for (String key : requiredStrings) {
      if (!config.hasPath(key) || config.getString(key).isBlank()) {
        throw new IllegalArgumentException("Missing required key " + key + " (" + name + ")");
      }
    }

    for (String key : requiredNumbers) {
      if (!config.hasPath(key)) {
        throw new IllegalArgumentException("Missing required key " + key + " (" + name + ")");
      }
    }

    for (String key : requiredBooleans) {
      if (!config.hasPath(key)) {
        throw new IllegalArgumentException("Missing required key " + key + " (" + name + ")");
      }
    }

    for (String key : requiredStrings) {
      try {
        config.getString(key);
      } catch (ConfigException.WrongType e) {
        throw new IllegalArgumentException("Wrong type for key " + key + " (expected string) (" + name + ")", e);
      }
    }

    for (String key : requiredNumbers) {
      try {
        config.getNumber(key);
      } catch (ConfigException.WrongType e) {
        throw new IllegalArgumentException("Wrong type for key " + key + " (expected number) (" + name + ")", e);
      }
    }

    for (String key : requiredBooleans) {
      try {
        config.getBoolean(key);
      } catch (ConfigException.WrongType e) {
        throw new IllegalArgumentException("Wrong type for key " + key + " (expected boolean) (" + name + ")", e);
      }
    }

    for (String key : optionalStrings) {
      if (config.hasPath(key)) {
        try {
          config.getString(key);
        } catch (ConfigException.WrongType e) {
          throw new IllegalArgumentException("Wrong type for key " + key + " (expected string) (" + name + ")", e);
        }
      }
    }

    for (String key : optionalNumbers) {
      if (config.hasPath(key)) {
        try {
          config.getNumber(key);
        } catch (com.typesafe.config.ConfigException.WrongType e) {
          throw new IllegalArgumentException("Wrong type for key " + key + " (expected number) (" + name + ")", e);
        }
      }
    }

    for (String key : optionalBooleans) {
      if (config.hasPath(key)) {
        try {
          config.getBoolean(key);
        } catch (com.typesafe.config.ConfigException.WrongType e) {
          throw new IllegalArgumentException("Wrong type for key " + key + " (expected boolean) (" + name + ")", e);
        }
      }
    }
  }

  /**
   * Class for building a {@link Spec} with required and optional keys.
   * 
   * This class collects desired keys by primitive type and produces an immutable
   * {@link Spec} on build.
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
     * Builds the configured spec instance.
     *
     * @return A {@link Spec} containing the accumulated required and optional keys.
     */
    public Spec build() {
      return this.spec;
    }
  }
}
