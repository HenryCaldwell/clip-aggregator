package info.henrycaldwell.streamline.admin.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

  private final JdbcTemplate jdbc;

  public RunRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  public List<RunRow> all(Long prevId, int limit, RunFilters filters) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, runner, status, published, started_at, ended_at, heartbeat_at
        FROM runs
        WHERE 1 = 1
        """);
    List<Object> params = new ArrayList<>();

    if (prevId != null) {
      sql.append(" AND id < ?");
      params.add(prevId);
    }

    if (filters.status() != null) {
      sql.append(" AND status = ?");
      params.add(filters.status());
    }

    if (filters.runner() != null) {
      sql.append(" AND runner = ?");
      params.add(filters.runner());
    }

    if (filters.from() != null) {
      sql.append(" AND started_at >= ?");
      params.add(filters.from().toString());
    }

    if (filters.to() != null) {
      sql.append(" AND started_at < ?");
      params.add(filters.to().toString());
    }

    sql.append(" ORDER BY id DESC LIMIT ?");
    params.add(limit);

    return jdbc.query(sql.toString(), this::mapRow, params.toArray());
  }

  public Optional<RunRow> one(long id) {
    String sql = """
        SELECT id, runner, status, published, started_at, ended_at, heartbeat_at
        FROM runs
        WHERE id = ?;
        """;
    List<RunRow> rows = jdbc.query(sql, this::mapRow, id);

    return rows.stream().findFirst();
  }

  public boolean exists(long id) {
    String sql = """
        SELECT COUNT(*)
        FROM runs
        WHERE id = ?;
        """;
    Integer count = jdbc.queryForObject(sql, Integer.class, id);

    return count != null && count > 0;
  }

  private RunRow mapRow(ResultSet result, int rowNum) throws SQLException {
    int publishedValue = result.getInt("published");
    Integer published = result.wasNull() ? null : publishedValue;

    String endedAtString = result.getString("ended_at");
    Instant endedAt = endedAtString != null ? Instant.parse(endedAtString) : null;

    String heartbeatAtString = result.getString("heartbeat_at");
    Instant heartbeatAt = heartbeatAtString != null ? Instant.parse(heartbeatAtString) : null;

    return new RunRow(
        result.getLong("id"),
        result.getString("runner"),
        result.getString("status"),
        published,
        Instant.parse(result.getString("started_at")),
        endedAt,
        heartbeatAt);
  }
}
