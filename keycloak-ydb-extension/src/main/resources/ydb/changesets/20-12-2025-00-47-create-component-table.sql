CREATE TABLE IF NOT EXISTS COMPONENT
(
    `ID` Utf8 NOT NULL,
    `NAME` Utf8,
    `PARENT_ID` Utf8,
    `PROVIDER_ID` Utf8,
    `PROVIDER_TYPE` Utf8,
    `REALM_ID` Utf8,
    `SUB_TYPE` Utf8,

    INDEX idx_component_realm GLOBAL ON (REALM_ID),
    INDEX idx_component_provider_type GLOBAL ON (PROVIDER_TYPE),
    PRIMARY KEY (ID)
);
