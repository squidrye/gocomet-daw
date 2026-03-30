CREATE TABLE payments (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ride_id         UUID NOT NULL REFERENCES rides(id),
  amount          NUMERIC(10,2) NOT NULL,
  transaction_id  TEXT NOT NULL,
  status          TEXT NOT NULL DEFAULT 'SUCCESS' CHECK (status IN ('SUCCESS', 'FAILED')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_ride_id ON payments(ride_id);
