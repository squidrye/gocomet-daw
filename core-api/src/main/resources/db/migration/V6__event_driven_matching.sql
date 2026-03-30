-- Event-driven matching: remove MATCHED/LOCKED states, add ride_declines

-- Create ride_declines table for tracking driver rejections
CREATE TABLE ride_declines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ride_id UUID NOT NULL REFERENCES rides(id),
  driver_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  delete_info JSONB DEFAULT NULL
);

CREATE INDEX idx_ride_declines_ride_id ON ride_declines(ride_id);
CREATE INDEX idx_ride_declines_driver_id ON ride_declines(driver_id);

-- Reset any rides stuck in MATCHED(8) back to REQUESTED(7)
UPDATE rides SET status_type_id = 7, driver_id = NULL WHERE status_type_id = 8;

-- Reset any drivers stuck in LOCKED(5) back to AVAILABLE(4)
UPDATE users SET driver_status_type_id = 4 WHERE driver_status_type_id = 5;

-- Now safe to remove the type entries
DELETE FROM type WHERE id = 5;
DELETE FROM type WHERE id = 8;
