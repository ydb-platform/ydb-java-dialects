CREATE TABLE IF NOT EXISTS `AUTHENTICATION_FLOW`
(
    `ID`          Utf8 NOT NULL,
    `ALIAS`       Utf8,
    `DESCRIPTION` Utf8,
    `REALM_ID`    Utf8,
    `PROVIDER_ID` Utf8 NOT NULL DEFAULT "basic-flow",
    `TOP_LEVEL`   Bool NOT NULL DEFAULT false,
    `BUILT_IN`    Bool NOT NULL DEFAULT false,

    INDEX         idx_auth_flow_realm GLOBAL ON (`REALM_ID`),
    PRIMARY KEY (`ID`)
);
