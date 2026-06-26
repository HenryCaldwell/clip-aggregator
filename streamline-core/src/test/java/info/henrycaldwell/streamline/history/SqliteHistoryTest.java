package info.henrycaldwell.streamline.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import info.henrycaldwell.streamline.error.SpecException;

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

  private static String escape(Path path) {
    return path.toString().replace("\\", "\\\\");
  }
}
