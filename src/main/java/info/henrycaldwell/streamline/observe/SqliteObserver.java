package info.henrycaldwell.streamline.observe;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for observing runs via a SQLite database.
 *
 * This class stores run and attempt events in a local SQLite database file.
 */
public final class SqliteObserver extends AbstractObserver {

  private static final Spec SPEC = Spec.builder()
      .requiredString("databasePath")
      .build();

  private Connection connection;

  private final String databasePath;

  /**
   * Constructs a SqliteObserver.
   *
   * @param config A {@link Config} representing the observer configuration.
   */
  public SqliteObserver(Config config) {
    super(config, SPEC);

    this.databasePath = config.getString("databasePath");
  }

  /**
   * Initializes a SQLite connection and schema.
   *
   * @throws ComponentException if the database cannot be opened or initialized.
   */
  @Override
  public void start() {
    if (connection != null) {
      return;
    }

    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

      String createRunsSql = """
          CREATE TABLE IF NOT EXISTS runs (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            runner       TEXT NOT NULL,
            config       TEXT,
            status       TEXT,
            published    INTEGER,
            started_at   TEXT NOT NULL,
            ended_at     TEXT,
            heartbeat_at TEXT
          );
          """;

      String createAttemptsSql = """
          CREATE TABLE IF NOT EXISTS attempts (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            run_id     INTEGER NOT NULL,
            worker     TEXT NOT NULL,
            clip_id    TEXT NOT NULL,
            stage      TEXT NOT NULL,
            component  TEXT,
            status     TEXT,
            error      TEXT,
            started_at TEXT NOT NULL,
            ended_at   TEXT,
            FOREIGN KEY (run_id) REFERENCES runs(id)
          );
          """;

      try (Statement create = connection.createStatement()) {
        create.executeUpdate(createRunsSql);
        create.executeUpdate(createAttemptsSql);
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to open SQLite database",
          MapUtils.ofNullable("databasePath", databasePath),
          e);
    }
  }

  /**
   * Releases the SQLite connection acquired by {@link #start()}.
   *
   * @throws ComponentException if the database connection cannot be closed.
   */
  @Override
  public void stop() {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        throw new ComponentException(name, "Failed to close SQLite database connection",
            MapUtils.ofNullable("databasePath", databasePath), e);
      } finally {
        connection = null;
      }
    }
  }

  /**
   * Records the start of a run in the SQLite database.
   *
   * @param runner A string representing the runner name.
   * @param config A string representing the resolved configuration rendered as
   *               JSON, or {@code null}.
   * @return A long representing the run identifier.
   * @throws ComponentException if the database operation fails or the observer is
   *                            not started.
   */
  @Override
  public synchronized long runStart(String runner, String config) {
    if (connection == null) {
      throw new ComponentException(name, "Observer not started");
    }

    String insertSql = """
        INSERT INTO runs (runner, config, started_at, heartbeat_at)
        VALUES (?, ?, ?, ?);
        """;

    try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
      String now = Instant.now().toString();
      insert.setString(1, runner);
      insert.setString(2, config);
      insert.setString(3, now);
      insert.setString(4, now);
      insert.executeUpdate();

      try (ResultSet keys = insert.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new ComponentException(name, "Failed to start run in SQLite database",
              MapUtils.ofNullable("databasePath", databasePath, "runner", runner));
        }

        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to start run in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "runner", runner), e);
    }
  }

  /**
   * Records the end of a run in the SQLite database.
   *
   * @param runId     A long representing the run identifier.
   * @param status    A {@link RunStatus} representing the terminal run status.
   * @param published An integer representing the number of clips published.
   * @throws ComponentException if the database operation fails or the observer is
   *                            not started.
   */
  @Override
  public synchronized void runEnd(long runId, RunStatus status, int published) {
    if (connection == null) {
      throw new ComponentException(name, "Observer not started");
    }

    String updateSql = """
        UPDATE runs
        SET status = ?,
            published = ?,
            ended_at = ?
        WHERE id = ?;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, status.name().toLowerCase());
      update.setInt(2, published);
      update.setString(3, Instant.now().toString());
      update.setLong(4, runId);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to end run in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "runId", runId), e);
    }
  }

  /**
   * Records the start of an attempt in the SQLite database.
   *
   * @param runId     A long representing the run identifier.
   * @param worker    A string representing the worker name.
   * @param clip      A {@link ClipRef} representing the clip being processed.
   * @param stage     A {@link PipelineStage} representing the pipeline stage.
   * @param component A string representing the component name, or {@code null}.
   * @return A long representing the attempt identifier.
   * @throws ComponentException if the database operation fails or the observer is
   *                            not started.
   */
  @Override
  public synchronized long attemptStart(long runId, String worker, ClipRef clip, PipelineStage stage,
      String component) {
    String id = clip.id();

    if (connection == null) {
      throw new ComponentException(name, "Observer not started");
    }

    String insertSql = """
        INSERT INTO attempts (run_id, worker, clip_id, stage, component, started_at)
        VALUES (?, ?, ?, ?, ?, ?);
        """;

    try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
      insert.setLong(1, runId);
      insert.setString(2, worker);
      insert.setString(3, id);
      insert.setString(4, stage.name().toLowerCase());
      insert.setString(5, component);
      insert.setString(6, Instant.now().toString());
      insert.executeUpdate();

      try (ResultSet keys = insert.getGeneratedKeys()) {
        if (!keys.next()) {
          throw new ComponentException(name, "Failed to start attempt in SQLite database",
              MapUtils.ofNullable("databasePath", databasePath, "runId", runId, "clipId", id));
        }

        return keys.getLong(1);
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to start attempt in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "runId", runId, "clipId", id), e);
    }
  }

  /**
   * Records the end of an attempt in the SQLite database.
   *
   * @param attemptId A long representing the attempt identifier.
   * @param status    An {@link AttemptStatus} representing the terminal attempt
   *                  status.
   * @param error     A {@link Throwable} representing the failure cause, or
   *                  {@code null}.
   * @throws ComponentException if the database operation fails or the observer is
   *                            not started.
   */
  @Override
  public synchronized void attemptEnd(long attemptId, AttemptStatus status, Throwable error) {
    if (connection == null) {
      throw new ComponentException(name, "Observer not started");
    }

    String updateSql = """
        UPDATE attempts
        SET status = ?,
            error = ?,
            ended_at = ?
        WHERE id = ?;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, status.name().toLowerCase());
      update.setString(2, error != null ? error.toString() : null);
      update.setString(3, Instant.now().toString());
      update.setLong(4, attemptId);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to end attempt in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "attemptId", attemptId), e);
    }
  }

  /**
   * Records a heartbeat for a live run in the SQLite database.
   *
   * @param runId A long representing the run identifier.
   * @throws ComponentException if the database operation fails or the observer is
   *                            not started.
   */
  @Override
  public synchronized void heartbeat(long runId) {
    if (connection == null) {
      throw new ComponentException(name, "Observer not started");
    }

    String updateSql = """
        UPDATE runs
        SET heartbeat_at = ?
        WHERE id = ? AND status IS NULL;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, Instant.now().toString());
      update.setLong(2, runId);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to heartbeat in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "runId", runId), e);
    }
  }
}
