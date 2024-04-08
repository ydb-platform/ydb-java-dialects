## 1.0.2 ##

* Added duration format for `Interval` type and ISO for time types when loading data into tables.

## 1.0.1 ##

* Scan select databasechangelog

## 1.0.0 ##

* Fixed bug with distributed lock (setAutoCommit = false)
* Supported insert changeset with all YDB types

---

## 0.9.7 ##

* Supported NOT NULL statement
* Added support for YDB unsigned integer types

## 0.9.6 ##

* Fixed NullPointerException when load csv file

## 0.9.5 ##

* Supported loadData and loadUpdateData from CSV file.

## 0.9.4 ##

* Change liquibase-parent-pom on liquibase-core dependency

## 0.9.3 ##

* Shaded org.slf4j dependency from liquibase-parent-pom

## 0.9.2 ##

* Fixed bug with NullPointerException in CREATE INDEX generator method.

## 0.9.1 ##

* Supported "CREATE TABLE", "ALTER TABLE", "DROP TABLE" and "CREATE INDEX" SQL statements from .xml, .json and .yaml file formats.
* Added support for YDB primitive data types.
* Included support for DATABASECHANGELOG and DATABASECHANGELOGLOCK tables.