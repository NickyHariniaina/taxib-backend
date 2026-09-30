package com.nicky.hariniaina.conf;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared PostGIS database for ALL tests (including the generated FacadeIT, which cannot be edited).
 * Registered via AutoConfiguration.imports in test resources. Boot backs off its own DataSource in
 * favor of this one; Flyway migrates the container on startup.
 */
@AutoConfiguration
public class PostgisTestInfra {

  @Bean(destroyMethod = "stop")
  GenericContainer<?> postgis() {
    GenericContainer<?> container =
        new GenericContainer<>(DockerImageName.parse("postgis/postgis:16-3.4"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "taxib")
            .withEnv("POSTGRES_USER", "taxib")
            .withEnv("POSTGRES_PASSWORD", "taxib")
            .waitingFor(
                // times=2: first boot runs a temporary server for initdb that shuts
                // down again; the SECOND "ready" is the real postmaster. times=1
                // connects to a dying server (EOF/reset) — the classic pitfall.
                Wait.forLogMessage(".*database system is ready to accept connections.*", 2));
    container.start();
    return container;
  }

  @Bean
  DataSource dataSource(GenericContainer<?> postgis) {
    // sslmode=disable: the test container has no SSL; this skips the handshake
    // entirely. (Supabase keeps sslmode=require — see main application.yml.)
    return DataSourceBuilder.create()
        .driverClassName("org.postgresql.Driver")
        .url(
            "jdbc:postgresql://"
                + postgis.getHost()
                + ":"
                + postgis.getMappedPort(5432)
                + "/taxib?sslmode=disable")
        .username("taxib")
        .password("taxib")
        .build();
  }
}
