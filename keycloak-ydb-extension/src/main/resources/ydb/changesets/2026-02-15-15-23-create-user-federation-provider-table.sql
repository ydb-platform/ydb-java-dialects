CREATE TABLE IF NOT EXISTS USER_FEDERATION_PROVIDER
(
    `ID`                  Utf8 NOT NULL,
    `CHANGED_SYNC_PERIOD` Int32,
    `DISPLAY_NAME`        Utf8,
    `FULL_SYNC_PERIOD`    Int32,
    `LAST_SYNC`           Int32,
    `PRIORITY`            Int32,
    `PROVIDER_NAME`       Utf8,
    `REALM_ID`            Utf8,

    INDEX idx_usr_fed_prv_realm GLOBAL ON (REALM_ID),
    INDEX idx_usr_fed_prv_name GLOBAL ON (PROVIDER_NAME),
    INDEX idx_usr_fed_prv_priority GLOBAL ON (PRIORITY),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
);