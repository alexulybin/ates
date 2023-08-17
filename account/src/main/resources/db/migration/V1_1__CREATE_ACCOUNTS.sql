CREATE SEQUENCE account_sequence START 1 INCREMENT 1;

CREATE TABLE ACCOUNTS
(
    id              VARCHAR(32) PRIMARY KEY NOT NULL,
    public_id       VARCHAR(32) NOT NULL,
    user_id         VARCHAR(32),
    balance         INT8
);