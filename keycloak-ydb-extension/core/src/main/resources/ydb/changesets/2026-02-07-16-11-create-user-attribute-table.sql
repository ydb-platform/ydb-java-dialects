CREATE TABLE IF NOT EXISTS USER_ATTRIBUTE
(
--     maybe sybase-needs-something-here is not needed
--     because it was added in other changelog
    `ID`                         Utf8 NOT NULL DEFAULT "sybase-needs-something-here",
    `NAME`                       Utf8 NOT NULL,
    `VALUE`                      Utf8,
    `USER_ID`                    Utf8 NOT NULL,
    `LONG_VALUE_HASH`            String,
    `LONG_VALUE_HASH_LOWER_CASE` String,
    `LONG_VALUE`                 Utf8,

    INDEX idx_user_attribute GLOBAL ON (USER_ID),
    INDEX idx_user_attribute_name GLOBAL ON (NAME, VALUE),
    INDEX user_attr_long_values GLOBAL ON (LONG_VALUE_HASH, NAME),
    INDEX user_attr_long_values_lower_case GLOBAL ON (LONG_VALUE_HASH_LOWER_CASE, NAME),
--     FOREIGN KEY (USER_ID) REFERENCES USER_ENTITY (ID),
    PRIMARY KEY (ID)
);