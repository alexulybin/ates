CREATE SEQUENCE payment_sequence START 1 INCREMENT 1;

CREATE TABLE PAYMENTS
(
    id              VARCHAR(32) PRIMARY KEY NOT NULL,
    public_id       VARCHAR(32) NOT NULL,
    user_id         VARCHAR(32),
    task_id         VARCHAR(32),
    amount          INT8,
    type            VARCHAR,
    date_time       TIMESTAMP
);
