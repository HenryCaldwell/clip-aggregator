package info.henrycaldwell.aggregator.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for map operations.
 *
 * This class provides helpers for constructing and working with maps.
 */
public final class MapUtils {

  private MapUtils() {
  }

  /**
   * Builds an ordered map from alternating string keys and nullable values.
   *
   * @param pairs An array of objects representing alternating string keys and
   *              nullable values.
   * @return A {@link Map} representing the ordered key-value pairs.
   * @throws IllegalArgumentException if the arguments are invalid.
   */
  public static Map<String, Object> ofNullable(Object... pairs) {
    if (pairs == null) {
      throw new IllegalArgumentException("pairs must be provided");
    }

    if (pairs.length % 2 != 0) {
      throw new IllegalArgumentException("pairs must contain matched keys and values");
    }

    Map<String, Object> map = new LinkedHashMap<>();

    for (int i = 0; i < pairs.length; i += 2) {
      Object key = pairs[i];

      if (!(key instanceof String stringKey)) {
        throw new IllegalArgumentException("key must be a string");
      }

      map.put(stringKey, pairs[i + 1]);
    }

    return map;
  }
}
