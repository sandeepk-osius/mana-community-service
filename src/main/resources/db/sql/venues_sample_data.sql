-- Seeding venue table with baseline venues
-- Binds to 'Lakshmi''s Emperia' community (invite_code: LE-MY-HYD)
DO $$
DECLARE
    le_comm_id INT;
BEGIN
    SELECT id INTO le_comm_id FROM community WHERE invite_code = 'LE-MY-HYD';
    
    IF le_comm_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM venue WHERE name = 'LE Box Cricket') THEN
            INSERT INTO venue (name, venue_type, address, area, venue_category, city, capacity, community_id)
            VALUES ('LE Box Cricket', 'COMMUNITY', 'Coomunity Back Gate', 'Miyapur', 'APARTMENT', 'Hyderabad', 500, le_comm_id);
        END IF;
    END IF;
END $$;
