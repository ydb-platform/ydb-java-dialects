CREATE TABLE IF NOT EXISTS FED_USER_CREDENTIAL
(
    `ID`                  Utf8 NOT NULL,
    `SALT`                String,
    `TYPE`                Utf8,
    `CREATED_DATE`        Int64,
    `USER_ID`             Utf8 NOT NULL,
    `REALM_ID`            Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,
    `USER_LABEL`          Utf8,
    `SECRET_DATA`         Utf8,
    `CREDENTIAL_DATA`     Utf8,
    `PRIORITY`            Int32,

    INDEX idx_fu_credential GLOBAL ON (USER_ID, TYPE),
    INDEX idx_fu_credential_ru GLOBAL ON (REALM_ID, USER_ID),
    INDEX idx_fed_credential_realm GLOBAL ON (REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
);