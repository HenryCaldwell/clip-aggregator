package info.henrycaldwell.aggregator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MapUtilsTest {

  @Nested
  class OfNullable {

    @Test
    void returnsMapWithProvidedEntries() {
      Map<String, Object> result = MapUtils.ofNullable("name", "clip", "views", 123);

      assertEquals("clip", result.get("name"));
      assertEquals(123, result.get("views"));
    }

    @Test
    void preservesInsertionOrder() {
      Map<String, Object> result = MapUtils.ofNullable("first", 1, "second", 2, "third", 3);

      assertEquals(List.of("first", "second", "third"), new ArrayList<>(result.keySet()));
    }

    @Test
    void allowsNullValues() {
      Map<String, Object> result = MapUtils.ofNullable("sourcePath", null);

      assertTrue(result.containsKey("sourcePath"));
      assertNull(result.get("sourcePath"));
    }

    @Test
    void returnsEmptyMapForNoPairs() {
      Map<String, Object> result = MapUtils.ofNullable();

      assertTrue(result.isEmpty());
    }

    @Test
    void throwsOnNullPairs() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> MapUtils.ofNullable((Object[]) null));

      assertEquals("pairs must be provided", exception.getMessage());
    }

    @Test
    void throwsOnUnmatchedPairs() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> MapUtils.ofNullable("key"));

      assertEquals("pairs must contain matched keys and values", exception.getMessage());
    }

    @Test
    void throwsOnNonStringKey() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
          () -> MapUtils.ofNullable(123, "value"));

      assertEquals("key must be a string", exception.getMessage());
    }
  }
}
