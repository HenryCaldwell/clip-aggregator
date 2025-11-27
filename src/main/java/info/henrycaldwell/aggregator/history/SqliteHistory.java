package info.henrycaldwell.aggregator.history;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import com.typesafe.config.Config;

import info.henrycaldwell.aggregator.config.Spec;

/**
 * Class for tracking published clips using a SQLite database.
 *
 * This class stores claimed clips in a local SQLite database file.
 */
public final class SqliteHistory extends AbstractHistory {

  private static final Spec SPEC = Spec.builder()
      .requiredString("path")
      .build();

  private Connection connection;

  private final String path;

  /**
   * Constructs an SqliteHistory.
   *
   * @param config A {@link Config} representing the history block.
   */
  public SqliteHistory(Config config) {
    super(config, SPEC);

    this.path = config.getString("path");
  }

  /**
   * Initializes a SQLite connection and schema.
   *
   * @throws RuntimeException if the database cannot be opened or initialized.
   */
  @Override
  public void start() {
    if (connection != null) {
      return;
    }

    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + path);

      String sql = """
          CREATE TABLE IF NOT EXISTS clips (
            id        TEXT NOT NULL,
            runner    TEXT NOT NULL,
            claimed   TEXT NOT NULL,
            PRIMARY KEY (id, runner)
          );
          """;

      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(sql);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open SQLite history database (path: " + path + ")", e);
    }
  }

  /**
   * Releases the SQLite connection connection acquired by {@link #start()}.
   *
   * @throws RuntimeException if the database cannot be closed.
   */
  @Override
  public void stop() {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to close SQLite history database connection (" + name + ")", e);
      } finally {
        connection = null;
      }
    }
  }

  /**
   * Attempts to claim a clip.
   *
   * @param id     A string representing the clip identifier.
   * @param runner A string representing the runner name.
   * @return {@code true} if the clip was successfully claimed, {@code false}
   *         if the clip was already claimed.
   * @throws RuntimeException if the database operation fails.
   */
  @Override
  public boolean claim(String id, String runner) {
    String sql = """
        INSERT OR IGNORE INTO clips (id, runner, claimed)
        VALUES (?, ?, ?);
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, id);
      statement.setString(2, runner);
      statement.setString(3, Instant.now().toString());

      int updated = statement.executeUpdate();

      return updated == 1;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim clip in SQLite history (id: " + id + ", runner: " + runner + ")", e);
    }
  }
}
