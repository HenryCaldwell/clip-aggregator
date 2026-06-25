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
public class FetchRepository {

  private final JdbcTemplate jdbc;

  public FetchRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  public List<FetchRow> all(Long prevId, int limit, FetchFilters filters) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, run_id, retriever, status, error, clips, started_at, ended_at
        FROM fetches
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

    if (filters.retriever() != null) {
      sql.append(" AND retriever = ?");
      params.add(filters.retriever());
    }

    if (filters.runId() != null) {
      sql.append(" AND run_id = ?");
      params.add(filters.runId());
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

  public List<FetchRow> byRunId(long runId, Long prevId, int limit) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, run_id, retriever, status, error, clips, started_at, ended_at
        FROM fetches
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

  private FetchRow mapRow(ResultSet result, int rowNum) throws SQLException {
    int clipsValue = result.getInt("clips");
    Integer clips = result.wasNull() ? null : clipsValue;

    String endedAtString = result.getString("ended_at");
    Instant endedAt = endedAtString != null ? Instant.parse(endedAtString) : null;

    return new FetchRow(
        result.getLong("id"),
        result.getLong("run_id"),
        result.getString("retriever"),
        result.getString("status"),
        result.getString("error"),
        clips,
        Instant.parse(result.getString("started_at")),
        endedAt);
  }
}
