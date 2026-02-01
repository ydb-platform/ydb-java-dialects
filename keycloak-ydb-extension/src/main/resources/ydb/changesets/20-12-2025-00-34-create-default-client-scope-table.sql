CREATE TABLE IF NOT EXISTS `DEFAULT_CLIENT_SCOPE`
(
    `REALM_ID`      Utf8 NOT NULL,
    `SCOPE_ID`      Utf8 NOT NULL,
    `DEFAULT_SCOPE` Bool NOT NULL DEFAULT false,

    INDEX           idx_defcls_realm GLOBAL ON (`REALM_ID`),
    INDEX           idx_defcls_scope GLOBAL ON (`SCOPE_ID`),
    PRIMARY KEY (`REALM_ID`, `SCOPE_ID`)
);
