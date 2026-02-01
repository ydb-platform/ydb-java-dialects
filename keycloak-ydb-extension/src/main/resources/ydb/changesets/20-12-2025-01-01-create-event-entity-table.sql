CREATE TABLE IF NOT EXISTS EVENT_ENTITY
(
    `ID`                      Utf8 NOT NULL,
    `CLIENT_ID`               Utf8,
    `DETAILS_JSON`            Utf8,
    `ERROR`                   Utf8,
    `IP_ADDRESS`              Utf8,
    `REALM_ID`                Utf8,
    `SESSION_ID`              Utf8,
    `EVENT_TIME`              Int64,
    `TYPE`                    Utf8,
    `USER_ID`                 Utf8,
    `DETAILS_JSON_LONG_VALUE` Text,

    INDEX idx_event_time GLOBAL ON (`REALM_ID`, `EVENT_TIME`),
    INDEX idx_event_entity_user_id_type GLOBAL ON (`USER_ID`, `TYPE`, `EVENT_TIME`),
    PRIMARY KEY (`ID`)
);
