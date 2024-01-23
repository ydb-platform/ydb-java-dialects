[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Ftech%2Fydb%2Fdialects%2Fhibernate-ydb-dialect%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/tech.ydb.dialects/hibernate-ydb-dialect)
[![CI](https://img.shields.io/github/actions/workflow/status/ydb-platform/ydb-java-dialects/ci-hibernate-dialect.yaml?branch=main&label=CI)](https://github.com/ydb-platform/ydb-java-dialects/actions/workflows/ci-hibernate-dialect.yaml)

# YDB Dialect for Hibernate 6+

This project provides a custom Hibernate dialect
that allows you to integrate Hibernate ORM with Yandex Database (YDB).
This enables you to take advantage of Hibernate's powerful ORM
features while using YDB as your underlying database.

## Features

- Full CRUD (Create, Read, Update and Delete) support for YDB.
- Custom type mappings to utilize YDB's data types.
- Support for YDB-specific features and functions.

## Getting Started

### Requirements

To use this Hibernate YDB Dialect, you'll need:

- Java 17 or above.
- Hibernate version 6+
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance

### Installation

For Maven, add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>tech.ydb.dialects</groupId>
    <artifactId>hibernate-ydb-dialect</artifactId>
    <!-- Set actual version -->
    <version>${hibernate.ydb.dialect.version}</version> 
</dependency>
```

For Gradle, add the following to your build.gradle (or build.gradle.kts):

```groovy
dependencies {
    implementation 'tech.ydb.dialects:hibernate-ydb-dialect:$version' // Set actual version
}
```

### Configuration

Configure Hibernate to use the custom YDB dialect
by updating your hibernate.cfg.xml:

```xml
<property name="hibernate.dialect">tech.ydb.hibernate.dialect.YdbDialect</property>
```

Or, if you are using programmatic configuration:

```java
public static Configuration basedConfiguration() {
    return new Configuration()
            .setProperty(AvailableSettings.DRIVER, YdbDriver.class.getName())
            .setProperty(AvailableSettings.DIALECT, YdbDialect.class.getName());
}
```

## Usage

Use this custom dialect just like any other Hibernate dialect.
Map your entity classes to database tables and use Hibernate's
session factory to perform database operations.

## Integration with Spring Data JPA

Configure Spring Data JPA with Hibernate to use custom YDB dialect
by updating your application.properties:

```properties
spring.jpa.properties.hibernate.dialect=tech.ydb.hibernate.dialect.YdbDialect

spring.datasource.driver-class-name=tech.ydb.jdbc.YdbDriver
spring.datasource.url=jdbc:ydb:grpc://localhost:2136/local
```

An example of a simple Spring Data JPA repository can be found at the following
[link](https://github.com/ydb-platform/ydb-java-examples/tree/master/jdbc/spring-data-jpa).

## Known Limitations

In the section [NOTES.md](./NOTES.md), we list all the current dialect limitations
and provide solutions to them.

## Support and Contact

For support, you can open issues in the repository issue tracker with tag `hibernate-v6`.
