# Spring YDB Retry

## Overview

This project is a Spring Boot auto-configuration module that provides automatic retry
for transactional operations with [YDB](https://ydb.tech).

### Features

- Automatic retry of failed `@Transactional` methods on YDB retryable status codes
- `@YdbTransactional` annotation with per-method retry settings (maxRetries, backoff, idempotency)
- Dual backoff strategy (fast/slow) with jitter tailored to YDB error semantics
- Idempotent mode for extended retry coverage on non-deterministic status codes
- Fully configurable via `application.properties`

## Getting Started

### Requirements

- Java 21 or above
- Spring Boot 3.4+ / Spring Framework 6.2+
- [YDB JDBC Driver](https://github.com/ydb-platform/ydb-jdbc-driver)
- Access to a YDB Database instance

### Installation

For Maven, add the following dependency to your pom.xml:

```xml
<dependency>
    <groupId>tech.ydb</groupId>
    <artifactId>spring-ydb-retry</artifactId>
    <!-- Set actual version -->
    <version>${spring-ydb-retry.version}</version>
</dependency>
```

For Gradle, add the following to your build.gradle (or build.gradle.kts):

```groovy
dependencies {
    implementation 'tech.ydb:spring-ydb-retry:$version' // Set actual version
}
```

## Usage

The module is auto-configured via Spring Boot. Once the dependency is on the classpath,
all `@Transactional` (and `@YdbTransactional`) methods are intercepted with retry logic.

### Annotation

Use `@YdbTransactional` as a drop-in replacement for `@Transactional` with additional
retry parameters:

```java
@YdbTransactional(maxRetries = 5, idempotent = true)
public void save(User user) {
    // retried up to 5 times on YDB retryable errors
}
```

### Configuration

Configure retry behavior in `application.properties`:

```properties
# Enable/disable retry (default: true)
ydb.transaction.retry.enabled=true

# Maximum retry attempts (default: 10)
ydb.transaction.retry.max-retries=10

# Backoff settings for slow errors
ydb.transaction.retry.slow-backoff-base-ms=50
ydb.transaction.retry.slow-cap-backoff-ms=5000

# Backoff settings for fast errors
ydb.transaction.retry.fast-backoff-base-ms=5
ydb.transaction.retry.fast-cap-backoff-ms=500

```

Idempotent-only retry is configured per method via `@YdbTransactional(idempotent = true)`.
