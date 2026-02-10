CREATE TABLE IF NOT EXISTS FED_USER_ROLE_MAPPING
(
    `ROLE_ID`             Utf8 NOT NULL,
    `USER_ID`             Utf8 NOT NULL,
    `REALM_ID`            Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,

    INDEX idx_fu_role_mapping GLOBAL ON (USER_ID, ROLE_ID),
    INDEX idx_fu_role_mapping_ru GLOBAL ON (REALM_ID, USER_ID),
    INDEX idx_fed_role_mapping_role GLOBAL ON (ROLE_ID),
    INDEX idx_fed_role_mapping_realm GLOBAL ON (REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
--     FOREIGN KEY (ROLE_ID) REFERENCES KEYCLOAK_ROLE (ID),
    PRIMARY KEY (ROLE_ID, USER_ID)
);