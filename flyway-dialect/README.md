[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fdialects%2Fflyway-ydb-dialect%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb.dialects/flyway-ydb-dialect)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-dialects/ci-flyway-dialect.yaml?branch=main&label=CI)](https://github.com/ydb-platform/ydb-java-dialects/actions/workflows/ci-flyway-dialect.yaml)

# YDB Flyway Dialect

This project allows for automated database migration to [YDB](https://ydb.tech/docs/en/), 
using the [Flyway](https://documentation.red-gate.com/fd/) migration tool.

Flyway simplifies database migrations and provides version control 
for database schema changes, which is incredibly useful 
in team-based development environments.

### Features

- Support main Flyway commands: `migrate`, `info`, `repair`, `validate`, and `clean`
- Safe when used in parallel

## Getting Started

To use this YDB Flyway Dialect, you'll need:

- Java 17 or above
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance
- Flyway 10 or above

### Installation in Java application.

For Maven, add the following dependency to your pom.xml:

```xml
<!-- Set an actual versions -->

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>${flyway.core.version}</version>
</dependency>

<dependency>
    <groupId>tech.ydb.jdbc</groupId>
    <artifactId>ydb-jdbc-driver</artifactId>
    <version>${ydb.jdbc.version}</version>
</dependency>

<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>flyway-ydb-dialect</artifactId>
    <version>${flyway.ydb.dialect.version}</version>
</dependency>
```

For Gradle, add the following to your build.gradle (or build.gradle.kts):

```groovy
dependencies {
    // Set actual versions
    implementation "org.flywaydb:flyway-core:$flywayCoreVersion"
    implementation "tech.ydb.dialects:flyway-ydb-dialect:$flywayYdbDialecVersion"
    implementation "tech.ydb.jdbc:ydb-jdbc-driver:$ydbJdbcVersion"
}
```

### Installation in Flyway CLI

Mac OS:

```bash
# install flyway
# $(which flyway)

cp flyway-ydb-dialect-${version-dialect}.jar ./libexec/lib
cp ydb-jdbc-driver-shaded-${version-jdbc}.jar ./libexec/drivers
```

## Usage

Use this custom dialect just like any other Flyway dialect.

## Integration with Spring Boot

```properties
spring.datasource.url=jdbc:ydb:grpc://localhost:2136/local
```

An example of a simple Spring Boot Flyway can be found at the following
[link](https://github.com/ydb-platform/ydb-java-examples/tree/master/jdbc/spring-flyway-app).

## Limitations

To understand what SQL constructs YDB can perform,
see the [documentation](https://ydb.tech/docs/en/yql/reference/) for the query language.

## Authorization

See [connect to YDB](../README.md/#connect-to-ydb).

## Support and Contact

For support, you can open issues in the repository issue tracker with tag `flyway`.
