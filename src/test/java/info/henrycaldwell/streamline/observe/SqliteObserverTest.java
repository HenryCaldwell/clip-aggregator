package info.henrycaldwell.streamline.observe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

public class SqliteObserverTest {

  private static final ClipRef CLIP = new ClipRef("clip-1", null, null, null, null, 0, null);

  @TempDir
  Path tempDir;

  @Nested
  class Constructor {

    @Test
    void acceptsMinimalConfig() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));

      assertDoesNotThrow(() -> new SqliteObserver(config));
    }

    @Test
    void throwsOnMissingDatabasePath() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteObserver(config));

      assertTrue(exception.getMessage().contains("Missing required key"));
      assertTrue(exception.getMessage().contains("key=databasePath"));
    }

    @Test
    void throwsOnWrongTypeForDatabasePath() {
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = [observer.db]
          """);

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteObserver(config));

      assertTrue(exception.getMessage().contains("Incorrect key type (expected string)"));
      assertTrue(exception.getMessage().contains("key=databasePath"));
    }

    @Test
    void throwsOnUnknownKey() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          extra = value
          """.formatted(escape(database)));

      SpecException exception = assertThrows(SpecException.class, () -> new SqliteObserver(config));

      assertTrue(exception.getMessage().contains("Unknown configuration key"));
      assertTrue(exception.getMessage().contains("key=extra"));
    }
  }

  @Nested
  class Start {

    @Test
    void createsDatabaseSchema() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      observer.start();

      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
        try (ResultSet result = connection.getMetaData().getTables(null, null, "runs", null)) {
          assertTrue(result.next());
        }
        try (ResultSet result = connection.getMetaData().getTables(null, null, "attempts", null)) {
          assertTrue(result.next());
        }
      } finally {
        observer.stop();
      }
    }

    @Test
    void doesNothingWhenAlreadyStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      observer.start();

      try {
        assertDoesNotThrow(observer::start);
      } finally {
        observer.stop();
      }
    }
  }

  @Nested
  class Stop {

    @Test
    void doesNothingWhenNotStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      assertDoesNotThrow(observer::stop);
    }

    @Test
    void allowsObserverToStartAgainAfterStopping() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      observer.start();
      observer.stop();

      assertDoesNotThrow(observer::start);
      observer.stop();
    }
  }

  @Nested
  class RunStart {

    @Test
    void throwsWhenObserverIsNotStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> observer.runStart("runner", null));

      assertTrue(exception.getMessage().contains("Observer not started"));
    }

    @Test
    void insertsStartedRun() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);
      observer.start();

      try {
        long runId = observer.runStart("runner", "{\"key\":\"value\"}");

        RunRow row = runRow(database, runId);

        assertEquals("runner", row.runner());
        assertEquals("{\"key\":\"value\"}", row.config());
        assertNull(row.status());
        assertNull(row.published());
        assertNotNull(row.startedAt());
        assertNull(row.endedAt());
      } finally {
        observer.stop();
      }
    }
  }

  @Nested
  class RunEnd {

    @Test
    void throwsWhenObserverIsNotStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> observer.runEnd(1L, RunStatus.COMPLETED, 0));

      assertTrue(exception.getMessage().contains("Observer not started"));
    }

    @Test
    void marksStartedRunCompleted() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);
      observer.start();

      try {
        long runId = observer.runStart("runner", null);
        observer.runEnd(runId, RunStatus.COMPLETED, 3);

        RunRow row = runRow(database, runId);

        assertEquals("completed", row.status());
        assertEquals(3, row.published());
        assertNotNull(row.endedAt());
      } finally {
        observer.stop();
      }
    }
  }

  @Nested
  class AttemptStart {

    @Test
    void throwsWhenObserverIsNotStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> observer.attemptStart(1L, "worker", CLIP, PipelineStage.DOWNLOAD, "yt-dlp"));

      assertTrue(exception.getMessage().contains("Observer not started"));
    }

    @Test
    void insertsStartedAttempt() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);
      observer.start();

      try {
        long runId = observer.runStart("runner", null);
        long attemptId = observer.attemptStart(runId, "worker", CLIP, PipelineStage.DOWNLOAD, "yt-dlp");

        AttemptRow row = attemptRow(database, attemptId);

        assertEquals(runId, row.runId());
        assertEquals("worker", row.worker());
        assertEquals("clip-1", row.clipId());
        assertEquals("download", row.stage());
        assertEquals("yt-dlp", row.component());
        assertNull(row.status());
        assertNull(row.error());
        assertNotNull(row.startedAt());
        assertNull(row.endedAt());
      } finally {
        observer.stop();
      }
    }
  }

  @Nested
  class AttemptEnd {

    @Test
    void throwsWhenObserverIsNotStarted() {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);

      ComponentException exception = assertThrows(ComponentException.class,
          () -> observer.attemptEnd(1L, AttemptStatus.SUCCESS, null));

      assertTrue(exception.getMessage().contains("Observer not started"));
    }

    @Test
    void marksStartedAttemptSucceeded() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);
      observer.start();

      try {
        long runId = observer.runStart("runner", null);
        long attemptId = observer.attemptStart(runId, "worker", CLIP, PipelineStage.DOWNLOAD, "yt-dlp");

        observer.attemptEnd(attemptId, AttemptStatus.SUCCESS, null);

        AttemptRow row = attemptRow(database, attemptId);

        assertEquals("success", row.status());
        assertNull(row.error());
        assertNotNull(row.endedAt());
      } finally {
        observer.stop();
      }
    }

    @Test
    void marksStartedAttemptFailed() throws Exception {
      Path database = tempDir.resolve("observer.db");
      Config config = ConfigFactory.parseString("""
          name = observer
          type = sqlite
          databasePath = "%s"
          """.formatted(escape(database)));
      SqliteObserver observer = new SqliteObserver(config);
      observer.start();

      try {
        long runId = observer.runStart("runner", null);
        long attemptId = observer.attemptStart(runId, "worker", CLIP, PipelineStage.DOWNLOAD, "yt-dlp");

        RuntimeException error = new RuntimeException("download failed");
        observer.attemptEnd(attemptId, AttemptStatus.FAILURE, error);

        AttemptRow row = attemptRow(database, attemptId);

        assertEquals("failure", row.status());
        assertEquals(error.toString(), row.error());
        assertNotNull(row.endedAt());
      } finally {
        observer.stop();
      }
    }
  }

  private static String escape(Path path) {
    return path.toString().replace("\\", "\\\\");
  }

  private static RunRow runRow(Path database, long id) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT runner, config, status, published, started_at, ended_at
            FROM runs
            WHERE id = ?
            """)) {
      statement.setLong(1, id);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());

        int publishedValue = result.getInt("published");
        Integer published = result.wasNull() ? null : publishedValue;

        return new RunRow(
            result.getString("runner"),
            result.getString("config"),
            result.getString("status"),
            published,
            result.getString("started_at"),
            result.getString("ended_at"));
      }
    }
  }

  private static AttemptRow attemptRow(Path database, long id) throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        PreparedStatement statement = connection.prepareStatement("""
            SELECT run_id, worker, clip_id, stage, component, status, error, started_at, ended_at
            FROM attempts
            WHERE id = ?
            """)) {
      statement.setLong(1, id);

      try (ResultSet result = statement.executeQuery()) {
        assertTrue(result.next());

        return new AttemptRow(
            result.getLong("run_id"),
            result.getString("worker"),
            result.getString("clip_id"),
            result.getString("stage"),
            result.getString("component"),
            result.getString("status"),
            result.getString("error"),
            result.getString("started_at"),
            result.getString("ended_at"));
      }
    }
  }

  private record RunRow(
      String runner,
      String config,
      String status,
      Integer published,
      String startedAt,
      String endedAt) {
  }

  private record AttemptRow(
      long runId,
      String worker,
      String clipId,
      String stage,
      String component,
      String status,
      String error,
      String startedAt,
      String endedAt) {
  }
}
