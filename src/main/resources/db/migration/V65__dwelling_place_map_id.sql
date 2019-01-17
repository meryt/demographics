ALTER TABLE dwelling_places ADD COLUMN map_id TEXT;

COMMENT ON COLUMN dwelling_places.map_id IS 'For towns, identifies which map is used; for dwellings, which polygon on the map';
