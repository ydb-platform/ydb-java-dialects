CREATE TABLE IF NOT EXISTS FED_USER_CONSENT
(
    `ID`                      Utf8 NOT NULL,
    `CLIENT_ID`               Utf8,
    `USER_ID`                 Utf8 NOT NULL,
    `REALM_ID`                Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID`     Utf8,
    `CREATED_DATE`            Int64,
    `LAST_UPDATED_DATE`       Int64,
    `CLIENT_STORAGE_PROVIDER` Utf8,
    `EXTERNAL_CLIENT_ID`      Utf8,

    INDEX idx_fu_consent_ru GLOBAL ON (REALM_ID, USER_ID),
    INDEX idx_fu_cnsnt_ext GLOBAL ON (USER_ID, CLIENT_STORAGE_PROVIDER, EXTERNAL_CLIENT_ID),
    INDEX idx_fu_consent GLOBAL ON (USER_ID, CLIENT_ID),
    INDEX idx_fed_consent_realm GLOBAL ON (REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
    );