package info.henrycaldwell.streamline.admin.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AttemptRepository {

  private final JdbcTemplate jdbc;

  public AttemptRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  public List<AttemptRow> byRunId(long runId, Long prevId, int limit) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, run_id, worker, clip_id, stage, component, status, error, started_at, ended_at
        FROM attempts
        WHERE run_id = ?
        """);
    List<Object> params = new ArrayList<>();
    params.add(runId);

    if (prevId != null) {
      sql.append(" AND id < ?");
      params.add(prevId);
    }

    sql.append(" ORDER BY id DESC LIMIT ?");
    params.add(limit);

    return jdbc.query(sql.toString(), this::mapRow, params.toArray());
  }

  private AttemptRow mapRow(ResultSet result, int rowNum) throws SQLException {
    String endedAtString = result.getString("ended_at");
    Instant endedAt = endedAtString != null ? Instant.parse(endedAtString) : null;

    return new AttemptRow(
        result.getLong("id"),
        result.getLong("run_id"),
        result.getString("worker"),
        result.getString("clip_id"),
        result.getString("stage"),
        result.getString("component"),
        result.getString("status"),
        result.getString("error"),
        Instant.parse(result.getString("started_at")),
        endedAt);
  }
}
