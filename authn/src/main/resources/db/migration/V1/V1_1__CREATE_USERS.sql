CREATE SEQUENCE user_sequence START 1 INCREMENT 1;

CREATE TABLE USERS
(
    id          INT8 PRIMARY KEY NOT NULL,
    public_id   UUID NOT NULL,
    login       VARCHAR(100),
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    password    VARCHAR(100),
    role        VARCHAR(100),
    email       VARCHAR(255),
    created     TIMESTAMP,
    updated     TIMESTAMP
);

