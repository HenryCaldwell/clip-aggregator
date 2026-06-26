package info.henrycaldwell.streamline.history;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.typesafe.config.Config;

import info.henrycaldwell.streamline.config.Spec;
import info.henrycaldwell.streamline.core.ClipRef;
import info.henrycaldwell.streamline.error.ComponentException;
import info.henrycaldwell.streamline.util.MapUtils;

/**
 * Class for tracking clips via a SQLite database.
 *
 * This class stores published clip identifiers in a local SQLite database file.
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
            PRIMARY KEY (id, runner)
          );
          """;

      try (Statement create = connection.createStatement()) {
        create.executeUpdate(createClipsSql);
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
   * Checks whether a clip has been published in the SQLite history.
   *
   * @param clip   A {@link ClipRef} representing the clip to check.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip has been published, {@code false} if the
   *         clip has not been published.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized boolean contains(ClipRef clip, String runner) {
    String id = clip.id();

    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String selectSql = """
        SELECT 1 FROM clips WHERE id = ? AND runner = ?;
        """;

    try (PreparedStatement select = connection.prepareStatement(selectSql)) {
      select.setString(1, id);
      select.setString(2, runner);

      try (ResultSet result = select.executeQuery()) {
        return result.next();
      }
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to check contains in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }

  /**
   * Records a clip as published in the SQLite history.
   *
   * @param clip   A {@link ClipRef} representing the published clip.
   * @param runner A string representing the runner name.
   * @throws ComponentException if the database operation fails or the history is
   *                            not started.
   */
  @Override
  public synchronized void add(ClipRef clip, String runner) {
    String id = clip.id();

    if (connection == null) {
      throw new ComponentException(name, "History not started");
    }

    String insertSql = """
        INSERT INTO clips (id, runner)
        VALUES (?, ?);
        """;

    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
      insert.setString(1, id);
      insert.setString(2, runner);
      insert.executeUpdate();
    } catch (SQLException e) {
      throw new ComponentException(name, "Failed to add in SQLite database",
          MapUtils.ofNullable("databasePath", databasePath, "clipId", id, "runner", runner), e);
    }
  }
}
