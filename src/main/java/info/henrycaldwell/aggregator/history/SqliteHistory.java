package info.henrycaldwell.aggregator.history;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Map;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;
import info.henrycaldwell.aggregator.error.ComponentException;

/**
 * Class for tracking clips via a SQLite database.
 *
 * This class stores claimed clip identifiers in a local SQLite database file.
 */
public final class SqliteHistory extends AbstractHistory {

  private static final Spec SPEC = Spec.builder()
      .requiredString("databasePath")
      .build();

  private Connection connection;

  private final String databasePath;

  /**
   * Constructs a SqliteHistory.
   *
   * @param config A {@link Config} representing the history configuration.
   */
  public SqliteHistory(Config config) {
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

      String createSql = """
          CREATE TABLE IF NOT EXISTS clips (
            id           TEXT NOT NULL,
            runner       TEXT NOT NULL,
            status       TEXT NOT NULL,
            attempts     INTEGER NOT NULL DEFAULT 0,
            error        TEXT,
            claimed_at   TEXT,
            prepared_at  TEXT,
            published_at TEXT,
            PRIMARY KEY (id, runner)
          );
          """;

      try (Statement create = connection.createStatement()) {
        create.executeUpdate(createSql);
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to open SQLite database", Map.of("databasePath", databasePath), e);
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
            Map.of("databasePath", databasePath), e);
      } finally {
        connection = null;
      }
    }
  }

  /**
   * Attempts to claim a clip in the SQLite history.
   *
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip was successfully claimed, {@code false} if
   *         the clip was already published.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized boolean claim(String id, String runner) {
    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String selectSql = """
        SELECT status FROM clips WHERE id = ? AND runner = ?
        """;

    try (PreparedStatement select = connection.prepareStatement(selectSql)) {
      select.setString(1, id);
      select.setString(2, runner);

      ResultSet result = select.executeQuery();

      if (result.next()) {
        String status = result.getString("status");

        if ("published".equals(status)) {
          return false;
        }

        String updateSql = """
            UPDATE clips
            SET status = 'claimed',
              claimed_at = ?,
              error = NULL
            WHERE id = ? AND runner = ?
            """;

        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
          update.setString(1, Instant.now().toString());
          update.setString(2, id);
          update.setString(3, runner);
          update.executeUpdate();
        }
      } else {
        String insertSql = """
            INSERT INTO clips (id, runner, status, attempts, claimed_at)
            VALUES (?, ?, 'claimed', 0, ?);
            """;

        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
          insert.setString(1, id);
          insert.setString(2, runner);
          insert.setString(3, Instant.now().toString());
          insert.executeUpdate();
        }
      }

      return true;
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to claim in SQLite database",
          Map.of("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as successfully prepared in the SQLite history.
   * 
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void prepare(String id, String runner) {
    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String updateSql = """
        UPDATE clips
        SET status = 'prepared',
            prepared_at = ?
        WHERE id = ? AND runner = ?;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, Instant.now().toString());
      update.setString(2, id);
      update.setString(3, runner);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to prepare in SQLite database",
          Map.of("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as successfully published in the SQLite history.
   * 
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void publish(String id, String runner) {
    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String updateSql = """
        UPDATE clips
        SET status = 'published',
            attempts = attempts + 1,
            published_at = ?
        WHERE id = ? AND runner = ?;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, Instant.now().toString());
      update.setString(2, id);
      update.setString(3, runner);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to publish in SQLite database",
          Map.of("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as failed in the SQLite history.
   * 
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @param error  A string representing the human-readable error message, or
   *               {@code null}.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void fail(String id, String runner, String error) {
    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String updateSql = """
        UPDATE clips
        SET status = 'failed',
            attempts = attempts + 1,
            error = ?
        WHERE id = ? AND runner = ?;
        """;

    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, error);
      update.setString(2, id);
      update.setString(3, runner);
      update.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to fail in SQLite database",
          Map.of("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }
}
