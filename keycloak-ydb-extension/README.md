# Keycloak YDB extension

## Overview

Keycloak extension to use [YDB](https://ydb.tech/) as the main database. Keycloak does not support YDB as a built-in database (see [Configuring the database](https://www.keycloak.org/server/db)); this extension provides the JDBC driver and Hibernate dialect.

## Configuration

When using YDB you must avoid giving the YDB URL to Keycloak’s default datasource (it would fail with “Driver does not support the provided URL”). Use:

| Variable | Value | Purpose |
|----------|--------|---------|
| `KC_DB` | `dev-file` | Built-in datasource uses dev-file; it never sees the YDB URL. |
| `KC_YDB_URL` | `jdbc:ydb:grpc://host:2136/database` | JDBC URL used by this extension. |
| `KC_COMMUNITY_DATASTORE_YDB_ENABLED` | `true` | Enables this extension’s JPA provider. |
| `KC_SPI_CONNECTIONS_JPA_QUARKUS_ENABLED` | `false` | Disables the default Quarkus JPA provider so H2 is not used. |
| `KC_SPI_CONNECTIONS_LIQUIBASE_QUARKUS_ENABLED` | `false` | Disables the default Quarkus Liquibase provider (migrations are run by this extension). |

Alternative: you can set `KC_DB_URL` instead of `KC_YDB_URL`; the extension will use it when the YDB profile is enabled. Then still set `KC_DB=dev-file` so the default pool is not created with that URL.

## Getting started

1. Build and package the extension, then put the JAR in Keycloak’s `providers` directory (or use the Docker setup below).
2. Set the environment variables above and start Keycloak.

### Local development with Docker

From the project root:

```bash
./run-keycloak-with-ydb.sh
```

This builds the extension, copies the JAR to `docker/providers/`, and starts Keycloak + YDB via `docker/docker-compose.yml`.