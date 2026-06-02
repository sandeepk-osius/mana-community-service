-- Seeding player_category table with baseline categories
-- Binds to the 'GENERAL' community (id is looked up dynamically)
DO $$
DECLARE
    gen_comm_id INT;
BEGIN
    SELECT id INTO gen_comm_id FROM community WHERE invite_code = 'GENERAL';
    
    IF gen_comm_id IS NOT NULL THEN
        -- Conditional inserts to prevent duplicate names
        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Boy''s Under 19') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Boy''s Under 19', 'BOYS', 'MALE', 0, 9, gen_comm_id, 'DEFAULT', 'Sample Boy''s Under 19 category');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Men''s Above 19') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Men''s Above 19', 'MENS', 'MALE', 19, 45, gen_comm_id, 'DEFAULT', 'Sample Men''s Above 19 category');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Women''s Above 19') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Women''s Above 19', 'WOMENS', 'FEMALE', 18, 50, gen_comm_id, 'DEFAULT', 'Sample Women''s Above 19 category');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Girl''s Under 19') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Girl''s Under 19', 'GIRLS', 'FEMALE', 0, 19, gen_comm_id, 'DEFAULT', 'Sample Girl''s Under 19 category');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Kid''s Under 12') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Kid''s Under 12', 'KIDS', 'ALL', 5, 12, gen_comm_id, 'DEFAULT', 'Sample Kid''s Under 12 category');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM player_category WHERE name = 'Senior''s Above 45') THEN
            INSERT INTO player_category (name, category_type, gender, min_age, max_age, community_id, type, description)
            VALUES ('Senior''s Above 45', 'SENIORS', 'ALL', 45, 55, gen_comm_id, 'DEFAULT', 'Sample Senior''s Above 45 category');
        END IF;
    END IF;
END $$;
