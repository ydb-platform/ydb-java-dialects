CREATE TABLE IF NOT EXISTS OFFLINE_USER_SESSION
(
    `USER_SESSION_ID`      Utf8 NOT NULL,
    `USER_ID`              Utf8 NOT NULL,
    `REALM_ID`             Utf8 NOT NULL,
    `CREATED_ON`           Int32 NOT NULL,
    `OFFLINE_FLAG`         Utf8 NOT NULL,
    `DATA`                 Utf8,
    `LAST_SESSION_REFRESH` Int32 NOT NULL DEFAULT 0,
    `BROKER_SESSION_ID`    Utf8,
    `VERSION`              Int32 DEFAULT 0,

    INDEX idx_offline_uss_by_user GLOBAL ON (USER_ID, REALM_ID, OFFLINE_FLAG),
    INDEX idx_offline_uss_by_last_session_refresh GLOBAL ON (REALM_ID, OFFLINE_FLAG, LAST_SESSION_REFRESH),
    INDEX idx_offline_uss_by_broker_session_id GLOBAL ON (BROKER_SESSION_ID, REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (USER_SESSION_ID, OFFLINE_FLAG)
);