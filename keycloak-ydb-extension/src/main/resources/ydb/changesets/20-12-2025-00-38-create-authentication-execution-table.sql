CREATE TABLE IF NOT EXISTS `AUTHENTICATION_EXECUTION`
(
    `ID`                 Utf8 NOT NULL,
    `ALIAS`              Utf8,
    `AUTHENTICATOR`      Utf8,
    `REALM_ID`           Utf8,
    `FLOW_ID`            Utf8,
    `REQUIREMENT`        Int32,
    `PRIORITY`           Int32,
    `AUTHENTICATOR_FLOW` Bool NOT NULL DEFAULT false,
    `AUTH_FLOW_ID`       Utf8,
    `AUTH_CONFIG`        Utf8,

    INDEX                idx_auth_exec_realm_flow GLOBAL ON (`REALM_ID`, `FLOW_ID`),
    INDEX                idx_auth_exec_flow GLOBAL ON (`FLOW_ID`),
    PRIMARY KEY (`ID`)
);
