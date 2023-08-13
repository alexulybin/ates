INSERT INTO USERS (id, public_id, login, first_name, last_name, password , role, email, created, updated)
VALUES(user_sequence.nextval, random_uuid(), 'appadmin', 'Админ', 'Админостратов', '$2y$04$kgK5iKI1gdj0035K9W7F.u8/9iZM8.5qVrpCGOj8muz6wgQEbShja',
       'admin', 'appadmin@ates.com', now(), now());

INSERT INTO USERS (id, public_id, login, first_name, last_name, password , role, email, created, updated)
VALUES(user_sequence.nextval, random_uuid(), 'manager1', 'Манагер', 'Манагеров', '$2y$04$p6ygHBVw9Wl2SOKwr7/0XOdJFsscns7tBipri7czgvmvVDrC.Hifm',
       'manager', 'manager1@ates.com', now(), now());
