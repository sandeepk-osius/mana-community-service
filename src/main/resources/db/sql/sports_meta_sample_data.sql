-- sports_meta_sample_data.sql
-- Seeds the sports_meta table with default predefined sports.

INSERT INTO sports_meta (name, icon, format, active, created_at, updated_at) VALUES
('Badminton', '🏸', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Basketball', '🏀', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Beach Volleyball', '🏐', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Billiards', '🎱', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bowling', '🎳', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Carrom', '🎯', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chess', '♟️', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cricket (Tennis Ball)', '🏏', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cycling', '🚴', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dart', '🎯', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Foosball', '⚽', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Grass Volleyball', '🏐', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kabaddi', '🤼', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pickleball', '🏓', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pool', '🎱', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (100M)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (1500M)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (200M)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (400M)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (800M)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Running (Others)', '🏃', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Snooker', '🎱', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Soccer', '⚽', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Squash', '🎾', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Swimming Race', '🏊', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Table Tennis', '🏓', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tennis', '🎾', 'SINGLES', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Throwball', '🤾', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tug of War', '🪢', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Volleyball', '🏐', 'TEAM', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO UPDATE SET
  icon = EXCLUDED.icon,
  format = EXCLUDED.format,
  active = EXCLUDED.active,
  updated_at = CURRENT_TIMESTAMP;
