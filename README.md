[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)
[![Telegram](https://img.shields.io/badge/chat-on%20Telegram-2ba2d9.svg)](https://t.me/ydb_en)
[![WebSite](https://img.shields.io/badge/website-ydb.tech-blue.svg)](https://ydb.tech)

# YDB Dialects for Java ORM frameworks and migration tools

This repository contains dialects required for using Java-based Object-Relational 
Mapper (ORM) frameworks and popular migration tools for Yandex Database (YDB). 
It allows Java applications to interact with YDB using popular ORM libraries 
such as Hibernate, Spring Data, and JOOQ, as well as migration 
tools like Flyway and Liquibase.

## Supports ORM Frameworks

- *Hibernate 5*:
  Hibernate Dialect for YDB, for earlier versions (5.*).
  Designed to use YDB features while providing a
  familiar experience for Hibernate users.
  For more information, please visit the appropriate [module](./hibernate-dialect-v5).
- *Hibernate 6+*:
  Hibernate Dialect for YDB is designed to leverage features of YDB while
  providing a familiar experience to Hibernate users.
  For more information, please visit the appropriate [module](./hibernate-dialect).
- *Spring Data JDBC*:
  Spring Data JDBC extension for YDB.
  For more information, please visit the appropriate [module](./spring-data-jdbc-ydb).
- *JOOQ*:
  Small JOOQ dialect is designed to be used for [code generation](https://www.jooq.org/doc/3.19/manual/code-generation/)
  For more information, please visit the appropriate [module](./jooq-dialect).

## Supports migration tools

- *Liquibase*:
  For more information, please visit the [module](./liquibase-dialect).
- *Flyway*:
  For more information, please visit the [module](./flyway-dialect).

Each module is developed independently,
and the release process is separate from the others.

Each module has its own README.md file,
which describes the integration process and various limitations,
as well as CHANGELOG.md, which describes the releases.

## Connect to YDB

* Local or remote Docker (anonymous authentication):<br>`jdbc:ydb:grpc://localhost:2136/local`
* Self-hosted cluster:<br>`jdbc:ydb:grpcs://<host>:2135/Root/testdb?secureConnectionCertificate=file:~/myca.cer`
* Connect with token to the cloud instance:<br>`jdbc:ydb:grpcs://<host>:2135/path/to/database?token=file:~/my_token`
* Connect with service account to the cloud instance:<br>`jdbc:ydb:grpcs://<host>:2135/path/to/database?saFile=file:~/sa_key.json`

## Contributing

We welcome contributions from the community.
Please see our contributing guidelines before making a pull request.

## License

This repository is licensed under the Apache 2.0 License.

## Support

For any questions or issues with the ORM Java Dialects for YDB,
please open an issue on the GitHub issue tracker.

Enjoy using ORM Java Dialects with YDB!
