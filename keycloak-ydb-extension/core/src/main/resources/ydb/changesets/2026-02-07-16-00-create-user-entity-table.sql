CREATE TABLE IF NOT EXISTS USER_ENTITY
(
    `ID`                          Utf8 NOT NULL,
    `EMAIL`                       Utf8,
    `EMAIL_CONSTRAINT`            Utf8,
    `EMAIL_VERIFIED`              Bool NOT NULL DEFAULT false,
    `ENABLED`                     Bool NOT NULL DEFAULT false,
    `FEDERATION_LINK`             Utf8,
    `FIRST_NAME`                  Utf8,
    `LAST_NAME`                   Utf8,
    `REALM_ID`                    Utf8,
    `USERNAME`                    Utf8,
    `CREATED_TIMESTAMP`           Int64,
    `SERVICE_ACCOUNT_CLIENT_LINK` Utf8,
    `NOT_BEFORE`                  Int32 NOT NULL DEFAULT 0,

    INDEX idx_user_email GLOBAL ON (EMAIL),
    INDEX idx_user_service_account GLOBAL ON (REALM_ID, SERVICE_ACCOUNT_CLIENT_LINK),
--     CONSTRAINT `UK_DYKN684SL8UP1CRFEI6ECKHD7` GLOBAL UNIQUE ON (REALM_ID, EMAIL_CONSTRAINT),
--     CONSTRAINT `UK_RU8TT6T700S9V50BU18WS5HA6` GLOBAL UNIQUE ON (REALM_ID, USERNAME),
    PRIMARY KEY (ID)
);