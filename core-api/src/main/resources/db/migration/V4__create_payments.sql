CREATE TABLE payments (
  id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  ride_id               UUID        NOT NULL REFERENCES rides(id),
  amount                NUMERIC(10,2) NOT NULL,
  transaction_id        TEXT        NOT NULL,
  status_type_id        INT         NOT NULL DEFAULT 13 REFERENCES type(id),
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_ride_id ON payments(ride_id);
