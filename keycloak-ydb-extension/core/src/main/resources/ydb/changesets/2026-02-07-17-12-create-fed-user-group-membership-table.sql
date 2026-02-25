CREATE TABLE IF NOT EXISTS FED_USER_GROUP_MEMBERSHIP
(
    `GROUP_ID`            Utf8 NOT NULL,
    `USER_ID`             Utf8 NOT NULL,
    `REALM_ID`            Utf8 NOT NULL,
    `STORAGE_PROVIDER_ID` Utf8,

    INDEX idx_fu_group_membership GLOBAL ON (USER_ID, GROUP_ID),
    INDEX idx_fu_group_membership_ru GLOBAL ON (REALM_ID, USER_ID),
    INDEX idx_fed_group_membership_group GLOBAL ON (GROUP_ID),
    INDEX idx_fed_group_membership_realm GLOBAL ON (REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
--     FOREIGN KEY (GROUP_ID) REFERENCES KEYCLOAK_GROUP (ID),
    PRIMARY KEY (GROUP_ID, USER_ID)
);