CREATE TABLE IF NOT EXISTS FEDERATED_USER
(
    `ID`                  Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,
    `REALM_ID`            Utf8 NOT NULL,

    INDEX idx_federated_user_realm GLOBAL ON (REALM_ID),
    INDEX idx_federated_user_storage GLOBAL ON (STORAGE_PROVIDER_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
);