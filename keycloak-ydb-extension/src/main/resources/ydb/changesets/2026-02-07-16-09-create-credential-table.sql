CREATE TABLE IF NOT EXISTS CREDENTIAL
(
    `ID`              Utf8 NOT NULL,
    `SALT`            String,
    `TYPE`            Utf8,
    `USER_ID`         Utf8,
    `CREATED_DATE`    Int64,
    `USER_LABEL`      Utf8,
    `SECRET_DATA`     Utf8,
    `CREDENTIAL_DATA` Utf8,
    `PRIORITY`        Int32,
    `VERSION`         Int32 DEFAULT 0,

    INDEX idx_user_credential GLOBAL ON (USER_ID),
--     FOREIGN KEY (USER_ID) REFERENCES USER_ENTITY (ID),
    PRIMARY KEY (ID)
);