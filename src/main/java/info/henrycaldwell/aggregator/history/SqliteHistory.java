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

  private final Connection connection;

  /**
   * Constructs an SqliteHistory.
   *
   * @param config A {@link Config} representing the history block.
   * @throws RuntimeException if the database cannot be opened or initialized.
   */
  public SqliteHistory(Config config) {
    super(config, SPEC);

    String path = config.getString("path");
    try {
      this.connection = DriverManager.getConnection("jdbc:sqlite:" + path);
      init();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to open SQLite history database (path: " + path + ")", e);
    }
  }

  /**
   * Initializes the database schema if it does not already exist.
   *
   * @throws SQLException if schema creation fails.
   */
  private void init() throws SQLException {
    String sql = """
        CREATE TABLE IF NOT EXISTS clips (
          id        TEXT NOT NULL,
          runner    TEXT NOT NULL,
          publisher TEXT NOT NULL,
          claimed   TEXT NOT NULL,
          PRIMARY KEY (id, runner, publisher)
        );
        """;

    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  /**
   * Closes the underlying database connection.
   *
   * @throws RuntimeException if closing the database fails.
   */
  @Override
  public void close() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to close SQLite history database connection (" + name + ")", e);
    }
  }

  /**
   * Attempts to claim a clip.
   *
   * @param id        A string representing the clip identifier.
   * @param runner    A string representing the runner name.
   * @param publisher A string representing the publisher name.
   * @return {@code true} if the clip was successfully claimed, {@code false}
   *         if the clip was already claimed.
   * @throws RuntimeException if the database operation fails.
   */
  @Override
  public boolean claim(String id, String runner, String publisher) {
    String sql = """
        INSERT OR IGNORE INTO clips (id, runner, publisher, claimed)
        VALUES (?, ?, ?, ?);
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, id);
      statement.setString(2, runner);
      statement.setString(3, publisher);
      statement.setString(4, Instant.now().toString());

      int updated = statement.executeUpdate();

      return updated == 1;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to claim clip in SQLite history (id: " + id + ", runner: " + runner
          + ", publisher: " + publisher + ")", e);
    }
  }
}
