package info.henrycaldwell.streamline.admin.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {

  @Bean
  public DataSource dataSource(@Value("${streamline.observer.database-path}") String path) {
    return DataSourceBuilder.create().url("jdbc:sqlite:" + path).build();
  }
}
