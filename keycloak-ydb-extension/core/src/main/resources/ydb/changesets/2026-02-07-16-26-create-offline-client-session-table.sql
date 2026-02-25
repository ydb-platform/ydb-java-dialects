CREATE TABLE IF NOT EXISTS OFFLINE_CLIENT_SESSION
(
    `USER_SESSION_ID`         Utf8 NOT NULL,
    `CLIENT_ID`               Utf8 NOT NULL,
    `OFFLINE_FLAG`            Utf8 NOT NULL,
    `TIMESTAMP`               Int32,
    `DATA`                    Utf8,
    `CLIENT_STORAGE_PROVIDER` Utf8 NOT NULL DEFAULT "local",
    `EXTERNAL_CLIENT_ID`      Utf8 NOT NULL DEFAULT "local",
    `VERSION`                 Int32 DEFAULT 0,

    INDEX idx_offl_client_sess_client GLOBAL ON (CLIENT_ID),
    INDEX idx_offl_client_sess_user GLOBAL ON (USER_SESSION_ID),
    INDEX idx_offl_client_sess_ext_client GLOBAL ON (EXTERNAL_CLIENT_ID),
    PRIMARY KEY (USER_SESSION_ID, CLIENT_ID, CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID, OFFLINE_FLAG)
);