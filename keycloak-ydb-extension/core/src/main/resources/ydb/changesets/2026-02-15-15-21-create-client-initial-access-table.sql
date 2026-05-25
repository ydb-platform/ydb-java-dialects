CREATE TABLE IF NOT EXISTS CLIENT_INITIAL_ACCESS
(
    `ID`              Utf8 NOT NULL,
    `REALM_ID`        Utf8 NOT NULL,
    `TIMESTAMP`       Int32,
    `EXPIRATION`      Int32,
    `COUNT`           Int32,
    `REMAINING_COUNT` Int32,

    INDEX idx_client_init_acc_realm GLOBAL ON (REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
);