# YDB Dialect for Jimmer

## Overview

This project contains a custom Jimmer dialect
for a simple integration between Jimmer ORM and Yandex Database (YDB).
For more thorough integration it is recommended to use the YqlClientBuilder class 
for a custom JSqlClient.


### Features

- Custom type mappings to utilize YDB's data types.
- Support for YDB-specific features and functions.
- Transaction modes and isolation levels.
- YDB keyset pagination.

## Getting Started

### Requirements

To use this Hibernate YDB Dialect, you'll need:

- Java 17 or above.
- Jimmer version 0.9.117
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance

## Usage

```java
DataSource dataSource = new DriverManagerDataSource("jdbc:ydb:grpc://localhost:2136/local");
YqlClient yqlClient = getYqlClient(dataSource);
```