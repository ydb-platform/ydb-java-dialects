CREATE TABLE IF NOT EXISTS PROTOCOL_MAPPER
(
    `ID`                   Utf8 NOT NULL,
    `NAME`                 Utf8 NOT NULL,
    `PROTOCOL`             Utf8 NOT NULL,
    `PROTOCOL_MAPPER_NAME` Utf8 NOT NULL,
    `CLIENT_ID`            Utf8,
    `CLIENT_SCOPE_ID`      Utf8,

    INDEX idx_protocol_mapper_client GLOBAL ON (CLIENT_ID),
    INDEX idx_clscope_protmap GLOBAL ON (CLIENT_SCOPE_ID),
--     FOREIGN KEY (CLIENT_ID) REFERENCES CLIENT (ID),
--     FOREIGN KEY (CLIENT_SCOPE_ID) REFERENCES CLIENT_SCOPE (ID),
    PRIMARY KEY (ID)
);
