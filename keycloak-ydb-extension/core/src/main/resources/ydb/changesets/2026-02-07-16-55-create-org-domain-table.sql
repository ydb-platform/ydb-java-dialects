CREATE TABLE IF NOT EXISTS ORG_DOMAIN
(
    `ID`       Utf8 NOT NULL,
    `NAME`     Utf8 NOT NULL,
    `VERIFIED` Bool NOT NULL,
    `ORG_ID`   Utf8 NOT NULL,

    INDEX idx_org_domain_org_id GLOBAL ON (ORG_ID),
    PRIMARY KEY (ID, NAME)
);