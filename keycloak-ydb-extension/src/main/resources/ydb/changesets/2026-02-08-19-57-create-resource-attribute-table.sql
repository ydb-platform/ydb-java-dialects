CREATE TABLE IF NOT EXISTS RESOURCE_ATTRIBUTE
(
    `ID`          Utf8 NOT NULL DEFAULT "sybase-needs-something-here",
    `NAME`        Utf8 NOT NULL,
    `VALUE`       Utf8,
    `RESOURCE_ID` Utf8 NOT NULL,

    INDEX idx_resource_attr_resource GLOBAL ON (RESOURCE_ID),
    INDEX idx_resource_attr_name_value GLOBAL ON (NAME, VALUE),
--     FOREIGN KEY (RESOURCE_ID) REFERENCES RESOURCE_SERVER_RESOURCE (ID),
    PRIMARY KEY (ID)
);