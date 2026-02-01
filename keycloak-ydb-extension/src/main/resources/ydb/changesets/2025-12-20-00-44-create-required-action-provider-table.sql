CREATE TABLE IF NOT EXISTS `REQUIRED_ACTION_PROVIDER`
(
    `ID`             Utf8 NOT NULL,
    `ALIAS`          Utf8,
    `NAME`           Utf8,
    `REALM_ID`       Utf8,
    `ENABLED`        Bool NOT NULL DEFAULT false,
    `DEFAULT_ACTION` Bool NOT NULL DEFAULT false,
    `PROVIDER_ID`    Utf8,
    `PRIORITY`       Int32,

    INDEX            idx_req_act_prov_realm GLOBAL ON (`REALM_ID`),
    PRIMARY KEY (`ID`)
);
