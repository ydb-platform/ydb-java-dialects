CREATE TABLE IF NOT EXISTS IDENTITY_PROVIDER
(
    `INTERNAL_ID`                Utf8 NOT NULL,
    `ENABLED`                    Bool NOT NULL DEFAULT false,
    `PROVIDER_ALIAS`             Utf8,
    `PROVIDER_ID`                Utf8,
    `STORE_TOKEN`                Bool NOT NULL DEFAULT false,
    `AUTHENTICATE_BY_DEFAULT`    Bool NOT NULL DEFAULT false,
    `REALM_ID`                   Utf8,
    `ADD_TOKEN_ROLE`             Bool NOT NULL DEFAULT true,
    `TRUST_EMAIL`                Bool NOT NULL DEFAULT false,
    `FIRST_BROKER_LOGIN_FLOW_ID` Utf8,
    `POST_BROKER_LOGIN_FLOW_ID`  Utf8,
    `PROVIDER_DISPLAY_NAME`      Utf8,
    `LINK_ONLY`                  Bool NOT NULL DEFAULT false,
    `ORGANIZATION_ID`            Utf8,
    `HIDE_ON_LOGIN`              Bool DEFAULT false,

    INDEX idx_ident_prov_realm GLOBAL ON (REALM_ID),
    INDEX idx_idp_realm_org GLOBAL ON (REALM_ID, ORGANIZATION_ID),
    INDEX idx_idp_for_login GLOBAL ON (REALM_ID, ENABLED, LINK_ONLY, HIDE_ON_LOGIN, ORGANIZATION_ID),
--     CONSTRAINT `UK_2DAELWNIBJI49AVXSRTUF6XJ33` GLOBAL UNIQUE ON (PROVIDER_ALIAS, REALM_ID),
--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (INTERNAL_ID)
);