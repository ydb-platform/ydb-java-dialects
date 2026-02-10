CREATE TABLE IF NOT EXISTS BROKER_LINK
(
    `IDENTITY_PROVIDER`   Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,
    `REALM_ID`            Utf8 NOT NULL,
    `BROKER_USER_ID`      Utf8,
    `BROKER_USERNAME`     Utf8,
    `TOKEN`               Utf8,
    `USER_ID`             Utf8 NOT NULL,

    INDEX idx_broker_link_realm GLOBAL ON (REALM_ID),
    INDEX idx_broker_link_user GLOBAL ON (USER_ID),
    INDEX idx_broker_link_provider GLOBAL ON (IDENTITY_PROVIDER),
    INDEX idx_broker_link_broker_user GLOBAL ON (BROKER_USER_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (IDENTITY_PROVIDER, USER_ID)
);