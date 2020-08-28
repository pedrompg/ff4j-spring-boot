package com.github.pedrompg.ff4j;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.ff4j.FF4j;
import org.ff4j.audit.repository.JdbcEventRepository;
import org.ff4j.property.store.JdbcPropertyStore;
import org.ff4j.springjdbc.store.EventRepositorySpringJdbc;
import org.ff4j.springjdbc.store.FeatureStoreSpringJdbc;
import org.ff4j.springjdbc.store.PropertyStoreSpringJdbc;
import org.ff4j.store.JdbcFeatureStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.BadSqlGrammarException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ApplicationIntegrationTest {

  @Container private final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>();

  public HikariDataSource dataSource() {
    return DataSourceBuilder.create()
        .url(postgresqlContainer.getJdbcUrl())
        .username(postgresqlContainer.getUsername())
        .password(postgresqlContainer.getPassword())
        .type(HikariDataSource.class)
        .build();
  }

  private FF4j ff4jSpringJdbc(DataSource dataSource) {
    final FF4j ff4j = new FF4j();

    ff4j.setFeatureStore(new FeatureStoreSpringJdbc(dataSource));
    ff4j.setPropertiesStore(new PropertyStoreSpringJdbc(dataSource));
    ff4j.setEventRepository(new EventRepositorySpringJdbc(dataSource));
    ff4j.audit(true);
    ff4j.autoCreate(true);

    return ff4j;
  }

  private FF4j plainJdbc(DataSource dataSource) {
    final FF4j ff4j = new FF4j();

    ff4j.setFeatureStore(new JdbcFeatureStore(dataSource));
    ff4j.setPropertiesStore(new JdbcPropertyStore(dataSource));
    ff4j.setEventRepository(new JdbcEventRepository(dataSource));
    ff4j.audit(true);
    ff4j.autoCreate(true);

    return ff4j;
  }

  @Test
  void failsOnCallingCreateSchemaWithSpringJdbcTwice() {
    final FF4j ff4j = ff4jSpringJdbc(dataSource());
    ff4j.createSchema(); // first call is successful

    Assertions.assertThatThrownBy(ff4j::createSchema)
        .isInstanceOf(BadSqlGrammarException.class)
        .hasRootCauseMessage("ERROR: relation \"ff4j_properties\" already exists");
  }

  @Test
  void doesNotFailOnCallingCreateSchemaWithPlainJdbcTwice() {
    final FF4j ff4j = plainJdbc(dataSource());
    ff4j.createSchema(); // first calls is successful
    ff4j.createSchema(); // no error
  }
}
