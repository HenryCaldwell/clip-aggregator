package info.henrycaldwell.streamline.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class SqliteHistoryTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

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

      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
        try (ResultSet result = connection.getMetaData().getTables(null, null, "clips", null)) {
          assertTrue(result.next());
        }
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
  class Contains {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> history.contains(CLIP, "runner"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void returnsFalseWhenClipIsNotPublished() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        boolean result = history.contains(CLIP, "runner");

        assertFalse(result);
      } finally {
        history.stop();
      }
    }

    @Test
    void returnsTrueWhenClipIsPublished() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.add(CLIP, "runner");

        boolean result = history.contains(CLIP, "runner");

        assertTrue(result);
      } finally {
        history.stop();
      }
    }
  }

  @Nested
  class Add {

    @Test
    void throwsWhenHistoryIsNotStarted() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);

      ComponentException exception = assertThrows(ComponentException.class, () -> history.add(CLIP, "runner"));

      assertTrue(exception.getMessage().contains("History not started"));
    }

    @Test
    void insertsPublishedClip() throws Exception {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.add(CLIP, "runner");

        assertTrue(clipExists(database, "clip-1", "runner"));
      } finally {
        history.stop();
      }
    }

    @Test
    void throwsWhenClipIsAlreadyPublished() {
      Path database = tempDir.resolve("history.db");
      Config config = ConfigFactory.parseString("""
          name = history
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteHistory history = new SqliteHistory(config);
      history.start();

      try {
        history.add(CLIP, "runner");

        ComponentException exception = assertThrows(ComponentException.class, () -> history.add(CLIP, "runner"));

        assertTrue(exception.getMessage().contains("Failed to add"));
        assertTrue(exception.getMessage().contains("clipId=clip-1"));
      } finally {
        history.stop();
      }
    }
  }

  private static String escape(Path path) {
    return path.toString().replace("\\", "\\\\");
  }

  private static boolean clipExists(Path database, String id, String runner) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT 1 FROM clips WHERE id = ? AND runner = ?
            """)) {
      statement.setString(1, id);
      statement.setString(2, runner);

      try (ResultSet result = statement.executeQuery()) {
        return result.next();
      }
    }
  }
}
