-- Add search radius tracking for expanding radius dispatch
ALTER TABLE rides ADD COLUMN search_radius_km DOUBLE PRECISION NOT NULL DEFAULT 5.0;
