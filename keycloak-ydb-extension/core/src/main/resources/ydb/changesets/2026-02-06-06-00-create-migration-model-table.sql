CREATE TABLE IF NOT EXISTS MIGRATION_MODEL
(
    `ID`         Utf8 NOT NULL,
    `VERSION`    Utf8,
    `UPDATE_TIME` Int64 NOT NULL DEFAULT 0,

    INDEX uk_migration_version GLOBAL UNIQUE ON (`VERSION`),
    INDEX uk_migration_update_time GLOBAL UNIQUE ON (`UPDATE_TIME`),
    INDEX idx_update_time GLOBAL ON (`UPDATE_TIME`),
    PRIMARY KEY (`ID`)
);
