[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fdialects%2Fliquibase-ydb-dialect%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb.dialects/liquibase-ydb-dialect)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-dialects/ci-liquibase-dialect.yaml?branch=main&label=CI)](https://github.com/ydb-platform/ydb-java-dialects/actions/workflows/ci-liquibase-dialect.yaml)

# YDB Liquibase Dialect

## Overview

This project utilizes Liquibase to manage database schema changes 
for the [Yandex Database (YDB)](https://ydb.tech/docs/en/), ensuring a consistent and version-controlled approach 
to database schema evolution. Liquibase is an open-source, 
database-independent library for tracking, managing, 
and applying database schema changes.

### Features

- Generate SQL (YQL) from .xml .json .yaml changelog files.
- Apply migration via YDB JDBC Driver.

## Getting Started

### Requirements

To use this Liquibase YDB Dialect, you'll need:

- Java 8 or above.
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance

### Installation in Java application.

For Maven, add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
    <version>${liquibase.core.version}</version>
</dependency>

<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>liquibase-ydb-dialect</artifactId>
    <!-- Set actual version -->
    <version>${liquibase.ydb.dialect.version}</version>
</dependency>
```

For Gradle, add the following to your build.gradle (or build.gradle.kts):

```groovy
dependencies {
    implementation 'org.liquibase:liquibase-core:4.24.0' // Set actual version
    implementation 'tech.ydb.dialects:liquibase-ydb-dialect:$version' // Set actual version
}
```

### Installation in Liquibase CLI

Mac OS:

```bash
# install liquibase

cp liquibase-ydb-dialect-${version-dialect}.jar /usr/local/opt/liquibase/internal/lib
cp ydb-jdbc-driver-shaded-${version-jdbc}.jar /usr/local/opt/liquibase/internal/lib
```

## Usage

Use this custom dialect just like any other Liquibase dialect.

## Integration with Spring Boot

```properties
spring.datasource.url=jdbc:ydb:grpc://localhost:2136/local

spring.liquibase.change-log=classpath:changelog.yaml
```

An example of a simple Spring Boot Liquibase can be found at the following
[link](https://github.com/ydb-platform/ydb-java-examples/tree/master/jdbc/spring-liquibase-app).

## Limitations

To understand what SQL constructs YDB can perform, 
see the [documentation](https://ydb.tech/docs/en/yql/reference/) for the query language.

## Support and Contact

For support, you can open issues in the repository issue tracker with tag `liquibase`.

