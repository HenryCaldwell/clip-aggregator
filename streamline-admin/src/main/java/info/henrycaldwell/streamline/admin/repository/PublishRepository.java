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
public class PublishRepository {

  private final JdbcTemplate jdbc;

  public PublishRepository(DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
  }

  public List<PublishRow> byRunId(long runId, Long prevId, int limit) {
    StringBuilder sql = new StringBuilder("""
        SELECT id, run_id, clip_id, publisher, uri, published_at
        FROM publishes
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

  private PublishRow mapRow(ResultSet result, int rowNum) throws SQLException {
    return new PublishRow(
        result.getLong("id"),
        result.getLong("run_id"),
        result.getString("clip_id"),
        result.getString("publisher"),
        result.getString("uri"),
        Instant.parse(result.getString("published_at")));
  }
}
