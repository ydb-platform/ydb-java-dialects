# YDB Spring Data JDBC Dialect

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fdialects%2Fspring-data-jdbc-ydb%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb.dialects/spring-data-jdbc-ydb)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-dialects/ci-spring-data-jdbc-ydb.yaml?branch=main&label=CI)](https://github.com/ydb-platform/ydb-java-dialects/actions/workflows/ci-spring-data-jdbc-ydb.yaml)

## Overview

This project is an extension for Spring Data JDBC 
that provides support for working with [YDB](https://ydb.tech).

### Features

- Full support for basic operations with Spring Data JDBC 
- Supported VIEW INDEX statement from @ViewIndex annotation on method your Repository
- @YdbType explicitly specifies the YDB data type (Json example in String type)

## Getting Started

### Requirements

To use this Spring Data JDBC YDB Dialect, you'll need:

- Java 17 or above.
- Spring Data JDBC 3+
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance

### Installation

For Maven, add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>spring-data-jdbc-ydb</artifactId>
    <!-- Set actual version -->
    <version>${spring.data.jdbc.ydb.version}</version> 
</dependency>
```

For Gradle, add the following to your build.gradle (or build.gradle.kts):

```groovy
dependencies {
    implementation 'tech.ydb.dialects:spring-data-jdbc-ydb:$version' // Set actual version
}
```

## Usage

Use this custom dialect just like any other DBMS.

## Configuration

Configure Spring Data JDBC with YDB by updating your application.properties:

```properties
spring.datasource.driver-class-name=tech.ydb.jdbc.YdbDriver
spring.datasource.url=jdbc:ydb:grpc://localhost:2136/local
```

Java configuration for @YdbType annotation:

```java
@Import(AbstractYdbJdbcConfiguration.class)
public class YdbJdbcConfiguration {}
```

An example of a simple Spring Data JDBC repository can be found at the following
[link](https://github.com/ydb-platform/ydb-java-examples/tree/master/jdbc/spring-data-jdbc).

## Support and Contact

For support, you can open issues in the repository issue tracker with tag `spring-data-jdbc`.