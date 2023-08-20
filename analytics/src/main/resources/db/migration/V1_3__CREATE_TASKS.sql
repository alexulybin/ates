CREATE SEQUENCE task_sequence START 1 INCREMENT 1;

CREATE TABLE TASKS
(
    id              VARCHAR(32) PRIMARY KEY NOT NULL,
    public_id       VARCHAR(32) NOT NULL,
    description     VARCHAR(100),
    assignee_id     VARCHAR(32),
    fee             INT,
    reward          VARCHAR(100)
);