[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ydb-platform/ydb-java-dialects/blob/main/LICENSE.md)

# ORM Java Dialects for YDB

This repository contains the dialects required for using Java-based ORM
frameworks with Yandex Database (YDB). It allows Java applications
to interact with YDB using popular Object-Relational Mapping (ORM)
libraries such as Hibernate, Spring Data and JOOQ.

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

Each module is developed independently, 
and the release process is separate from the others. 

Each module has its own README.md file, 
which describes the integration process and various limitations, 
as well as CHANGELOG.md, which describes the releases.

## Contributing

We welcome contributions from the community.
Please see our contributing guidelines before making a pull request.

## License

This repository is licensed under the Apache 2.0 License.

## Support

For any questions or issues with the ORM Java Dialects for YDB,
please open an issue on the GitHub issue tracker.

Enjoy using ORM Java Dialects with YDB!
