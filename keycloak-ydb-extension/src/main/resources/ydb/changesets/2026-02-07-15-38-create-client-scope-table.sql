CREATE TABLE IF NOT EXISTS CLIENT_SCOPE
(
    `ID`          Utf8 NOT NULL,
    `NAME`        Utf8,
    `REALM_ID`    Utf8,
    `DESCRIPTION` Utf8,
    `PROTOCOL`    Utf8,

--     in pg
--     INDEX idx_realm_clscope GLOBAL ON (REALM_ID),
--     CONSTRAINT `UK_CLI_SCOPE` GLOBAL UNIQUE ON (REALM_ID, NAME),
    INDEX idx_realm_clscope GLOBAL UNIQUE ON (REALM_ID, NAME),
    PRIMARY KEY (ID)
);