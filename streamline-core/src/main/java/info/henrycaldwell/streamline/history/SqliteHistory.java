package info.henrycaldwell.streamline.history;

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
import info.henrycaldwell.streamline.core.MediaRef;
import info.henrycaldwell.streamline.core.PublishRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.util.MapUtils;

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

      String createClipsSql = """
          CREATE TABLE IF NOT EXISTS clips (
            id          TEXT NOT NULL,
            runner      TEXT NOT NULL,
            status      TEXT NOT NULL,
            attempts    INTEGER NOT NULL DEFAULT 0,
            error       TEXT,
            claimed_at  TEXT,
            prepared_at TEXT,
            PRIMARY KEY (id, runner)
          );
          """;

      String createPublishesSql = """
          CREATE TABLE IF NOT EXISTS publishes (
            id           TEXT NOT NULL,
            runner       TEXT NOT NULL,
            publisher    TEXT NOT NULL,
            uri          TEXT,
            published_at TEXT,
            PRIMARY KEY (id, runner, publisher)
          );
          """;

      try (Statement create = connection.createStatement()) {
        create.executeUpdate(createClipsSql);
        create.executeUpdate(createPublishesSql);
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
   * Attempts to claim a clip in the SQLite history.
   *
   * @param clip   A {@link ClipRef} representing the clip to claim.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip was successfully claimed, {@code false} if
   *         the clip was already published.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized boolean claim(ClipRef clip, String runner) {
    String id = clip.id();

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
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as successfully prepared in the SQLite history.
   *
   * @param media  A {@link MediaRef} representing the prepared media.
   * @param runner A string representing the runner name.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void prepare(MediaRef media, String runner) {
    String id = media.clip().id();

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
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as successfully published in the SQLite history.
   *
   * @param ref       A {@link PublishRef} representing the published clip.
   * @param runner    A string representing the runner name.
   * @param publisher A string representing the publisher name.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void publish(PublishRef ref, String runner, String publisher) {
    String id = ref.clip().id();

    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String updateSql = """
        UPDATE clips
        SET status = 'published',
            attempts = attempts + 1
        WHERE id = ? AND runner = ?;
        """;

    String insertSql = """
        INSERT INTO publishes (id, runner, publisher, uri, published_at)
        VALUES (?, ?, ?, ?, ?);
        """;

    try {
      try (PreparedStatement update = connection.prepareStatement(updateSql)) {
        update.setString(1, id);
        update.setString(2, runner);
        update.executeUpdate();
      }

      try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
        insert.setString(1, id);
        insert.setString(2, runner);
        insert.setString(3, publisher);
        insert.setString(4, ref.uri() != null ? ref.uri().toString() : null);
        insert.setString(5, Instant.now().toString());
        insert.executeUpdate();
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to publish in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Marks a clip as failed in the SQLite history.
   *
   * @param clip   A {@link ClipRef} representing the failed clip.
   * @param runner A string representing the runner name.
   * @param error  A string representing the human-readable error message, or
   *               {@code null}.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void fail(ClipRef clip, String runner, String error) {
    String id = clip.id();

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
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }
}
