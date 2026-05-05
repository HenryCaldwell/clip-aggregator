package info.henrycaldwell.aggregator.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.aggregator.error.ComponentException;
import info.henrycaldwell.aggregator.error.SpecException;

public class SqliteHistoryTest {

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));

      assertDoesNotThrow(() -> new SqliteHistory(config));
    }

    @Test
    void throwsOnMissingDatabasePath() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteHistory(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=databasePath"));
    }

    @Test
    void throwsOnWrongTypeForDatabasePath() {
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = [history.db]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteHistory(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=databasePath"));
    }

    @Test
    void throwsOnUnknownKey() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          extra = value
          """.formatted(escape(database)));

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteHistory(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void createsDatabaseSchema() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      history.start();

      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
          ResultSet result = connection.getMetaData().getTables(null, null, "clips", null)) {
        assertTrue(result.next());
      } finally {
        history.stop();
      }
    }

    @Test
    void doesNothingWhenAlreadyStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      history.start();

      try {
        assertDoesNotThrow(history::start);
      } finally {
        history.stop();
      }
    }
  }

  @Nested
  class Stop {

    @Test
    void doesNothingWhenNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      assertDoesNotThrow(history::stop);
    }

    @Test
    void allowsHistoryToStartAgainAfterStopping() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      history.start();
      history.stop();

      assertDoesNotThrow(history::start);
      history.stop();
    }
  }

  @Nested
  class Claim {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> history.claim("clip-1", "runner"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void insertsClaimedClip() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        boolean result = history.claim("clip-1", "runner");

        assertTrue(result);

        Row row = row(database, "clip-1", "runner");

        assertEquals("claimed", row.status());
        assertEquals(0, row.attempts());
        assertNotNull(row.claimedAt());
      } finally {
        history.stop();
      }
    }

    @Test
    void reclaimsExistingNonPublishedClipAndClearsError() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.claim("clip-1", "runner");
        history.fail("clip-1", "runner", "boom");

        boolean result = history.claim("clip-1", "runner");

        assertTrue(result);

        Row row = row(database, "clip-1", "runner");

        assertEquals("claimed", row.status());
        assertEquals(1, row.attempts());
        assertEquals(null, row.error());
      } finally {
        history.stop();
      }
    }

    @Test
    void returnsFalseWhenClipWasAlreadyPublished() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.claim("clip-1", "runner");
        history.publish("clip-1", "runner");

        boolean result = history.claim("clip-1", "runner");

        assertFalse(result);
        assertEquals("published", row(database, "clip-1", "runner").status());
      } finally {
        history.stop();
      }
    }
  }

  @Nested
  class Prepare {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> history.prepare("clip-1", "runner"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void marksClaimedClipPrepared() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.claim("clip-1", "runner");
        history.prepare("clip-1", "runner");

        Row row = row(database, "clip-1", "runner");

        assertEquals("prepared", row.status());
        assertNotNull(row.preparedAt());
      } finally {
        history.stop();
      }
    }
  }

  @Nested
  class Publish {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> history.publish("clip-1", "runner"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void marksClaimedClipPublishedAndIncrementsAttempts() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.claim("clip-1", "runner");
        history.publish("clip-1", "runner");

        Row row = row(database, "clip-1", "runner");

        assertEquals("published", row.status());
        assertEquals(1, row.attempts());
        assertNotNull(row.publishedAt());
      } finally {
        history.stop();
      }
    }
  }

  @Nested
  class Fail {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> history.fail("clip-1", "runner", "boom"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void marksClaimedClipFailedAndIncrementsAttempts() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.claim("clip-1", "runner");
        history.fail("clip-1", "runner", "boom");

        Row row = row(database, "clip-1", "runner");
        assertEquals("failed", row.status());
        assertEquals(1, row.attempts());
        assertEquals("boom", row.error());
      } finally {
        history.stop();
      }
    }
  }

  private static String escape(Path path) {
    return path.toString().replace("\\", "\\\\");
  }

  private static Row row(Path database, String id, String runner) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT status, attempts, error, claimed_at, prepared_at, published_at
            FROM clips
            WHERE id = ? AND runner = ?
            """)) {
      statement.setString(1, id);
      statement.setString(2, runner);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());

        return new Row(
            result.getString("status"),
            result.getInt("attempts"),
            result.getString("error"),
            result.getString("claimed_at"),
            result.getString("prepared_at"),
            result.getString("published_at"));
      }
    }
  }

  private record Row(
      String status,
      int attempts,
      String error,
      String claimedAt,
      String preparedAt,
      String publishedAt) {
  }
}
