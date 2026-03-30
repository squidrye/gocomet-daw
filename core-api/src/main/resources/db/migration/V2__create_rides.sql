CREATE TABLE rides (
  id             UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
  rider_id       UUID             NOT NULL REFERENCES users(id),
  driver_id      UUID             REFERENCES users(id),
  pickup_lat     DOUBLE PRECISION NOT NULL,
  pickup_lng     DOUBLE PRECISION NOT NULL,
  dropoff_lat    DOUBLE PRECISION NOT NULL,
  dropoff_lng    DOUBLE PRECISION NOT NULL,
  status_type_id INT              NOT NULL DEFAULT 7 REFERENCES type(id),
  estimated_fare NUMERIC(10,2),
  final_fare     NUMERIC(10,2),
  started_at     TIMESTAMPTZ,
  completed_at   TIMESTAMPTZ,
  cancelled_at   TIMESTAMPTZ,
  created_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_rides_status_type_id ON rides(status_type_id);
CREATE INDEX idx_rides_rider_id       ON rides(rider_id);
CREATE INDEX idx_rides_driver_id      ON rides(driver_id);
