CREATE TABLE IF NOT EXISTS KEYCLOAK_ROLE
(
    `ID`                      Utf8 NOT NULL,
    `CLIENT_REALM_CONSTRAINT` Utf8,
    `CLIENT_ROLE`             Bool NOT NULL DEFAULT false,
    `DESCRIPTION`             Utf8,
    `NAME`                    Utf8,
    `REALM_ID`                Utf8,
    `CLIENT`                  Utf8,
    `REALM`                   Utf8,

    INDEX idx_keycloak_role_client GLOBAL ON (`CLIENT`),

--     foreign key to REALM
    INDEX fk_6vyqfe4cn4wlq8r6kt5vdsj5c GLOBAL ON (`REALM`),
    INDEX `UK_J3RWUVD56ONTGSUHOGM184WW2-2` GLOBAL UNIQUE ON (`NAME`, `CLIENT_REALM_CONSTRAINT`),
    PRIMARY KEY (`ID`)
);