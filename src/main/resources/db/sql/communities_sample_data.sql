-- Seeding communities table with baseline data
INSERT INTO community (name, type, city, state, area, subtype, invite_code)
VALUES ('GENERAL', 'GENERAL', 'Hyderabad', 'Telangana', 'Miaypur', 'GENERAL', 'GENERAL')
ON CONFLICT (invite_code) DO NOTHING;

INSERT INTO community (name, type, city, state, area, subtype, invite_code)
VALUES ('Lakshmi''s Emperia', 'APARTMENT', 'Hyderabad', 'Telangana', 'Miaypur', 'Gated Community', 'LE-MY-HYD')
ON CONFLICT (invite_code) DO NOTHING;
