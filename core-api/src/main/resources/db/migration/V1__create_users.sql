-- Type groups for enum segregation
CREATE TABLE type_group (
  id   INT  PRIMARY KEY,
  name TEXT NOT NULL
);

-- All enum values stored as integer-keyed types
CREATE TABLE type (
  id       INT  PRIMARY KEY,
  name     TEXT NOT NULL,
  group_id INT  NOT NULL REFERENCES type_group(id)
);

-- Group 1: user_role
INSERT INTO type_group (id, name) VALUES (1, 'user_role');
INSERT INTO type (id, name, group_id) VALUES
  (1, 'RIDER',  1),
  (2, 'DRIVER', 1);

-- Group 2: driver_status
INSERT INTO type_group (id, name) VALUES (2, 'driver_status');
INSERT INTO type (id, name, group_id) VALUES
  (3, 'OFFLINE',   2),
  (4, 'AVAILABLE', 2),
  (5, 'LOCKED',    2),
  (6, 'ON_TRIP',   2);

-- Group 3: ride_status
INSERT INTO type_group (id, name) VALUES (3, 'ride_status');
INSERT INTO type (id, name, group_id) VALUES
  (7,  'REQUESTED',   3),
  (8,  'MATCHED',     3),
  (9,  'ACCEPTED',    3),
  (10, 'IN_PROGRESS', 3),
  (11, 'COMPLETED',   3),
  (12, 'CANCELLED',   3);

-- Group 4: payment_status
INSERT INTO type_group (id, name) VALUES (4, 'payment_status');
INSERT INTO type (id, name, group_id) VALUES
  (13, 'SUCCESS', 4),
  (14, 'FAILED',  4);

CREATE TABLE users (
  id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  email                 TEXT        NOT NULL,
  password_hash         TEXT        NOT NULL,
  role_type_id          INT         NOT NULL REFERENCES type(id),
  driver_status_type_id INT         REFERENCES type(id),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
