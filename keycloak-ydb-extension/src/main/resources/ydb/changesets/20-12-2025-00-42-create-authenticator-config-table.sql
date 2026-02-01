CREATE TABLE IF NOT EXISTS `AUTHENTICATOR_CONFIG`
(
    `ID`       Utf8 NOT NULL,
    `ALIAS`    Utf8,
    `REALM_ID` Utf8,

    INDEX      idx_auth_config_realm GLOBAL ON (`REALM_ID`),
    PRIMARY KEY (`ID`)
);
