[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fdialects%2Fhibernate-ydb-dialect-v5%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb.dialects/jooq-ydb-dialect)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-dialects/ci-jooq-dialect.yaml?branch=main&label=CI)](https://github.com/ydb-platform/ydb-java-dialects/actions/workflows/ci-jooq-dialect.yaml)

# YDB JOOQ Dialect

## Overview

This project introduces a JOOQ dialect specifically tailored for the Yandex Database (YDB). JOOQ, a popular Java-based ORM tool for SQL-centric database interaction, is now equipped to leverage the unique capabilities of YDB, enabling developers to maintain a high level of type safety and SQL abstraction.

### Features

- Full support for all YDB data types during code generation
- Preserves the original file structure in generated code
- Advanced type safety features aligning with YDB's data types
- Support for YDB-specific SQL syntax and operations *(coming soon)*

## Getting Started

### Requirements

To utilize this JOOQ YDB Dialect, ensure you have:

- Java 11 or above.
- JOOQ 3.15 or higher.
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance.

### Installation

For Maven, add the following dependencies to your pom.xml:

```xml
<dependency>
    <groupId>org.jooq</groupId>
    <artifactId>jooq</artifactId>
    <version>${jooq.version}</version>
</dependency>

<dependency>
    <groupId>tech.ydb.jdbc</groupId>
    <artifactId>ydb-jdbc-driver</artifactId>
    <version>${ydb.jdbc.version}</version>
</dependency>

<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>jooq-ydb-dialect</artifactId>
    <version>${jooq.ydb.dialect.version}</version>
</dependency>
```
For Gradle, include in your build.gradle:

```groovy
dependencies {
    implementation "org.jooq:jooq:$jooqVersion"
    implementation "tech.ydb.dialects:jooq-ydb-dialect:$jooqYdbDialectVersion"
    implementation "tech.ydb.jdbc:ydb-jdbc-driver:$ydbJdbcVersion"
}
```

### Configuration
Configure the JOOQ runtime to use the YDB dialect and JDBC driver:

```java
String url = "jdbc:ydb:grpc://localhost:2136/local";
Connection conn = DriverManager.getConnection(url);

DSLContext dsl = new YdbDslContext(conn);
```

### XML config

To ensure successful code generation, it is essential to correctly configure the XML configuration file. Specifically, you must specify two mandatory fields for YDB: `strategy.name=tech.ydb.jooq.codegen.YdbGeneratorStrategy` and `database.name=tech.ydb.jooq.codegen.YdbDatabase`. Here is an example:

```xml
<configuration>
    <jdbc>
        <driver>tech.ydb.jdbc.YdbDriver</driver>
        <url>jdbc:ydb:grpc://localhost:2136/local</url>
        <user>$user</user>
        <password>$password</password>
    </jdbc>

    <generator>
        <name>org.jooq.codegen.JavaGenerator</name>

        <strategy>
            <name>tech.ydb.jooq.codegen.YdbGeneratorStrategy</name>
        </strategy>

        <database>
            <name>tech.ydb.jooq.codegen.YdbDatabase</name>

            <includes>.*</includes>

            <excludes></excludes>
        </database>

        <target>
            <packageName>ydb</packageName>
            <directory>./generated</directory>
        </target>
    </generator>
</configuration>
```
For more information, see [here](https://www.jooq.org/doc/latest/manual/code-generation/codegen-configuration/)

## Usage
Leverage the power of JOOQ to create, read, update, and delete operations in a type-safe manner. Utilize advanced querying capabilities with the strong SQL abstraction provided by JOOQ.

Integration with Spring Boot:
```properties
spring.datasource.url=jdbc:ydb:grpc://localhost:2136/local
```

## Limitations

To understand what SQL constructs YDB can perform,
see the [documentation](https://ydb.tech/docs/en/yql/reference/) for the query language.

## Authorization

See [connect to YDB](../README.md/#connect-to-ydb).

## Support and Contact

For support, you can open issues in the repository issue tracker with tag `jooq`.