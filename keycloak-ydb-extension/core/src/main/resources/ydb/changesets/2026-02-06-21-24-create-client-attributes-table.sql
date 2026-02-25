CREATE TABLE IF NOT EXISTS CLIENT_ATTRIBUTES
(
    `CLIENT_ID` Utf8 NOT NULL,
    `NAME`      Utf8 NOT NULL,
    `VALUE`     Utf8,

--     INDEX idx_client_att_by_name_value GLOBAL ON (`NAME`, SUBSTRING(`VALUE`, 1, 255)),
--     not implemented in ydb...
    PRIMARY KEY (CLIENT_ID, NAME)
);
