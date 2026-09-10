CREATE TABLE group (
    id Uuid,
    name String,
    PRIMARY KEY (id)
);

CREATE TABLE student (
    id Uuid,
    name String,
    group Uuid,
    PRIMARY KEY (id)
);