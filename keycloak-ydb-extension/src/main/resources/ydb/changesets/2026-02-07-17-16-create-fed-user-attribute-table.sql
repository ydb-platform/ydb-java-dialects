CREATE TABLE IF NOT EXISTS FED_USER_ATTRIBUTE
(
    `ID`                         Utf8 NOT NULL,
    `NAME`                       Utf8 NOT NULL,
    `USER_ID`                    Utf8 NOT NULL,
    `REALM_ID`                   Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID`        Utf8,
    `VALUE`                      Utf8,
    `LONG_VALUE_HASH`            String,
    `LONG_VALUE_HASH_LOWER_CASE` String,
    `LONG_VALUE`                 Utf8,

    INDEX idx_fu_attribute GLOBAL ON (USER_ID, REALM_ID, NAME),
    INDEX fed_user_attr_long_values GLOBAL ON (LONG_VALUE_HASH, NAME),
    INDEX fed_user_attr_long_values_lower_case GLOBAL ON (LONG_VALUE_HASH_LOWER_CASE, NAME),
    INDEX idx_fed_user_attr_realm GLOBAL ON (REALM_ID),
    INDEX idx_fed_user_attr_user GLOBAL ON (USER_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (ID)
);