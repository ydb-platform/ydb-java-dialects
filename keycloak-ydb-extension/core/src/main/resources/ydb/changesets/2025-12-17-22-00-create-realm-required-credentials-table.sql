create table IF NOT EXISTS REALM_REQUIRED_CREDENTIAL
(
    REALM_ID   Utf8  not null,
    TYPE       Utf8 not null,
    FORM_LABEL Utf8,
    INPUT      Bool      not null default false,
    SECRET     Bool      not null default false,

--     FOREIGN KEY (REALM_ID) REFERENCES REALM (ID),
    PRIMARY KEY (REALM_ID, TYPE)
);