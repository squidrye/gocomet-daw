ALTER TABLE users ADD COLUMN hash TEXT;
ALTER TABLE rides ADD COLUMN hash TEXT;
ALTER TABLE payments ADD COLUMN hash TEXT;
ALTER TABLE ride_declines ADD COLUMN hash TEXT;

CREATE UNIQUE INDEX idx_users_hash ON users(hash) WHERE delete_info IS NULL;
CREATE UNIQUE INDEX idx_rides_hash ON rides(hash) WHERE delete_info IS NULL;
CREATE UNIQUE INDEX idx_payments_hash ON payments(hash) WHERE delete_info IS NULL;
CREATE UNIQUE INDEX idx_ride_declines_hash ON ride_declines(hash) WHERE delete_info IS NULL;
