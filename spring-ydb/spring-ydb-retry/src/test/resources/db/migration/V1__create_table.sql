CREATE TABLE Users
(
    id        Int64,
    username  Text,
    firstname Text,
    lastname  Text,
    PRIMARY KEY (id),
    INDEX username_index GLOBAL ON (username)
)