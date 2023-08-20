CREATE SEQUENCE user_sequence START 1 INCREMENT 1;

CREATE TABLE USERS
(
    id          VARCHAR(32) PRIMARY KEY NOT NULL,
    public_id   VARCHAR(32) NOT NULL,
    login       VARCHAR(100),
    role        VARCHAR(100),
    email       VARCHAR(255)
);

