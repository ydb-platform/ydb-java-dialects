CREATE TABLE IF NOT EXISTS FED_USER_ROLE_MAPPING
(
    `ROLE_ID`             Utf8 NOT NULL,
    `USER_ID`             Utf8 NOT NULL,
    `REALM_ID`            Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,

    INDEX idx_fu_role_mapping GLOBAL ON (USER_ID, ROLE_ID),
    INDEX idx_fu_role_mapping_ru GLOBAL ON (REALM_ID, USER_ID),
    PRIMARY KEY (ROLE_ID, USER_ID)
);