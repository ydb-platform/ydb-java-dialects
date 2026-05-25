CREATE TABLE IF NOT EXISTS GROUP_ATTRIBUTE
(
    `ID`       Utf8 NOT NULL DEFAULT "sybase-needs-something-here",
    `NAME`     Utf8 NOT NULL,
    `VALUE`    Utf8,
    `GROUP_ID` Utf8 NOT NULL,

    INDEX idx_group_attr_group GLOBAL ON (GROUP_ID),
    INDEX idx_group_att_by_name_value GLOBAL ON (NAME, VALUE),
--     FOREIGN KEY (GROUP_ID) REFERENCES KEYCLOAK_GROUP (ID),
    PRIMARY KEY (ID)
);