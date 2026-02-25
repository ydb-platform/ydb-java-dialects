CREATE TABLE IF NOT EXISTS RESOURCE_SERVER_POLICY
(
    `ID`                 Utf8 NOT NULL,
    `NAME`               Utf8 NOT NULL,
    `DESCRIPTION`        Utf8,
    `TYPE`               Utf8 NOT NULL,
    `DECISION_STRATEGY`  Int16,
    `LOGIC`              Int16,
    `RESOURCE_SERVER_ID` Utf8 NOT NULL,
    `OWNER`              Utf8,

    INDEX idx_res_serv_pol_res_serv GLOBAL ON (RESOURCE_SERVER_ID),
    INDEX idx_res_serv_pol_type GLOBAL ON (TYPE),
    INDEX idx_res_serv_pol_owner GLOBAL ON (OWNER),
-- todo maybe add unique index `ON (NAME, RESOURCE_SERVER_ID)`
--     CONSTRAINT `UK_FRSRPT700S9V50BU18WS5HA6` GLOBAL UNIQUE ON (NAME, RESOURCE_SERVER_ID),
--     FOREIGN KEY (RESOURCE_SERVER_ID) REFERENCES RESOURCE_SERVER (ID),
    PRIMARY KEY (ID)
);