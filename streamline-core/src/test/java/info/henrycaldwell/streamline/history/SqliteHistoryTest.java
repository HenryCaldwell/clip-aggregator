package info.henrycaldwell.streamline.history;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
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
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.error.SpecException;

public class SqliteHistoryTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);
  private static final MediaRef MEDIA = new MediaRef(CLIP, null, null);
  private static final PublishRef PUBLISH = new PublishRef(CLIP, null);

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
        try (ResultSet result = connection.getMetaData().getTables(null, null, "publishes", null)) {
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

      ComponentException exception = assertThrows(ComponentException.class, () -> history.claim(CLIP, "runner"));

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
        boolean result = history.claim(CLIP, "runner");

        assertTrue(result);

        ClipRow row = clipRow(database, "clip-1", "runner");

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
        history.claim(CLIP, "runner");
        history.fail(CLIP, "runner", "boom");

        boolean result = history.claim(CLIP, "runner");

        assertTrue(result);

        ClipRow row = clipRow(database, "clip-1", "runner");

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
        history.claim(CLIP, "runner");
        history.publish(PUBLISH, "runner", "publisher");

        boolean result = history.claim(CLIP, "runner");

        assertFalse(result);
        assertEquals("published", clipRow(database, "clip-1", "runner").status());
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

      ComponentException exception = assertThrows(ComponentException.class, () -> history.prepare(MEDIA, "runner"));

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
        history.claim(CLIP, "runner");
        history.prepare(MEDIA, "runner");

        ClipRow row = clipRow(database, "clip-1", "runner");

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

      ComponentException exception = assertThrows(ComponentException.class,
          () -> history.publish(PUBLISH, "runner", "publisher"));

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
        PublishRef ref = new PublishRef(CLIP, URI.create("https://www.instagram.com/p/abc/"));
        history.claim(CLIP, "runner");
        history.publish(ref, "runner", "publisher");

        ClipRow row = clipRow(database, "clip-1", "runner");

        assertEquals("published", row.status());
        assertEquals(1, row.attempts());

        PublishRow publishRow = publishRow(database, "clip-1", "runner", "publisher");

        assertEquals("https://www.instagram.com/p/abc/", publishRow.uri());
        assertNotNull(publishRow.publishedAt());
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
          () -> history.fail(CLIP, "runner", "boom"));

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
        history.claim(CLIP, "runner");
        history.fail(CLIP, "runner", "boom");

        ClipRow row = clipRow(database, "clip-1", "runner");
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

  private static ClipRow clipRow(Path database, String id, String runner) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT status, attempts, error, claimed_at, prepared_at
            FROM clips
            WHERE id = ? AND runner = ?
            """)) {
      statement.setString(1, id);
      statement.setString(2, runner);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());

        return new ClipRow(
            result.getString("status"),
            result.getInt("attempts"),
            result.getString("error"),
            result.getString("claimed_at"),
            result.getString("prepared_at"));
      }
    }
  }

  private static PublishRow publishRow(Path database, String clipId, String runner, String publisher) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT uri, published_at
            FROM publishes
            WHERE id = ? AND runner = ? AND publisher = ?
            """)) {
      statement.setString(1, clipId);
      statement.setString(2, runner);
      statement.setString(3, publisher);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());

        return new PublishRow(
            result.getString("uri"),
            result.getString("published_at"));
      }
    }
  }

  private record ClipRow(
      String status,
      int attempts,
      String error,
      String claimedAt,
      String preparedAt) {
  }

  private record PublishRow(
      String uri,
      String publishedAt) {
  }
}
