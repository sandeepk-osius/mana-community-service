-- ============================================================================
-- SAMPLE DATA FOR MANA COMMUNITY AUCTION SYSTEM
-- Database: PostgreSQL
-- ============================================================================

DO $$ 
DECLARE 
    v_sport_id BIGINT;
    v_admin_id BIGINT;
    v_config_id BIGINT;
    v_owner1_id BIGINT;
    v_owner2_id BIGINT;
    v_owner3_id BIGINT;
    v_owner4_id BIGINT;
BEGIN
    -- 1. Insert Base Sports
    INSERT INTO sport_meta (name, icon, min_age, max_age, active, created_at)
    VALUES 
        ('Cricket', '🏏', 10, 60, true, NOW()),
        ('Football', '⚽', 10, 50, true, NOW()),
        ('Badminton', '🏸', 8, 70, true, NOW())
    ON CONFLICT (name) DO NOTHING;

    -- 2. Insert Sample Users (Admin and Team Owners)
    INSERT INTO app_user (email, full_name, phone, role, kyc_status, password_hash, created_at)
    VALUES 
        ('admin@manacommunity.com', 'Super Admin', '9999999990', 'SUPER_ADMIN', 'VERIFIED', '$2a$10$xyz...', NOW()),
        ('owner1@example.com', 'Vikram Singh', '9999999991', 'AUCTION_TEAM_OWNER', 'VERIFIED', '$2a$10$xyz...', NOW()),
        ('owner2@example.com', 'Anjali Desai', '9999999992', 'AUCTION_TEAM_OWNER', 'VERIFIED', '$2a$10$xyz...', NOW()),
        ('owner3@example.com', 'Rahul Verma', '9999999993', 'AUCTION_TEAM_OWNER', 'VERIFIED', '$2a$10$xyz...', NOW()),
        ('owner4@example.com', 'Priya Sharma', '9999999994', 'AUCTION_TEAM_OWNER', 'VERIFIED', '$2a$10$xyz...', NOW())
    ON CONFLICT (email) DO NOTHING;

    -- Get IDs for foreign keys
    SELECT id INTO v_sport_id FROM sport_meta WHERE name = 'Cricket' LIMIT 1;
    SELECT id INTO v_admin_id FROM app_user WHERE email = 'admin@manacommunity.com' LIMIT 1;
    
    SELECT id INTO v_owner1_id FROM app_user WHERE email = 'owner1@example.com' LIMIT 1;
    SELECT id INTO v_owner2_id FROM app_user WHERE email = 'owner2@example.com' LIMIT 1;
    SELECT id INTO v_owner3_id FROM app_user WHERE email = 'owner3@example.com' LIMIT 1;
    SELECT id INTO v_owner4_id FROM app_user WHERE email = 'owner4@example.com' LIMIT 1;

    -- 3. Insert Auction Config
    INSERT INTO auction_config (
        sport_id, season_name, auction_format, total_teams, total_players, 
        budget_per_team, base_price, bid_increment_default, bid_increment_threshold, 
        bid_increment_above, bid_timer_seconds, rtm_enabled, unsold_rule, 
        status, created_by, created_at, updated_at
    ) VALUES (
        v_sport_id, 'Premier League 2026', 'OPEN_AUCTION', 4, 20, 
        1000000, 5000, 1000, 50000, 
        5000, 30, true, 'ROTATION_AUCTION', 
        'ACTIVE', v_admin_id, NOW(), NOW()
    ) RETURNING id INTO v_config_id;

    -- 4. Insert Auction Categories
    INSERT INTO auction_config_category (config_id, category_name) VALUES 
        (v_config_id, 'BATSMEN'),
        (v_config_id, 'BOWLERS'),
        (v_config_id, 'ALL_ROUNDERS'),
        (v_config_id, 'WICKET_KEEPERS');

    -- 5. Insert Dispute Committee
    INSERT INTO auction_dispute_committee (config_id, member_name, user_id, role, added_at) VALUES 
        (v_config_id, 'Suresh K.', NULL, 'CHAIRPERSON', NOW()),
        (v_config_id, 'Super Admin', v_admin_id, 'COMMITTEE_MEMBER', NOW());

    -- 6. Insert Teams
    INSERT INTO auction_team (config_id, team_name, owner_name, owner_user_id, color_hex, total_budget, remaining_budget, created_at) VALUES 
        (v_config_id, 'Phoenix Strikers', 'Vikram Singh', v_owner1_id, '#f97316', 1000000, 1000000, NOW()),
        (v_config_id, 'Royal Challengers', 'Anjali Desai', v_owner2_id, '#16a085', 1000000, 1000000, NOW()),
        (v_config_id, 'Titan Smashers', 'Rahul Verma', v_owner3_id, '#e67e22', 1000000, 1000000, NOW()),
        (v_config_id, 'Storm Bringers', 'Priya Sharma', v_owner4_id, '#2e86de', 1000000, 1000000, NOW());

    -- 7. Insert Players
    INSERT INTO auction_player (config_id, player_name, category, player_role, age, base_price, stats_json, queue_order, status) VALUES 
        (v_config_id, 'Virat K.', 'BATSMEN', 'Right-Hand Bat', 35, 10000, '{"matches": 250, "runs": 12000, "strikeRate": 138}', 1, 'QUEUED'),
        (v_config_id, 'Rohit S.', 'BATSMEN', 'Right-Hand Bat', 36, 10000, '{"matches": 240, "runs": 10500, "strikeRate": 140}', 2, 'QUEUED'),
        (v_config_id, 'Jasprit B.', 'BOWLERS', 'Right-Arm Fast', 30, 8000, '{"matches": 150, "wickets": 200, "economy": 6.5}', 3, 'QUEUED'),
        (v_config_id, 'Hardik P.', 'ALL_ROUNDERS', 'Pace All-rounder', 30, 8000, '{"matches": 180, "runs": 3000, "wickets": 150}', 4, 'QUEUED'),
        (v_config_id, 'MS Dhoni', 'WICKET_KEEPERS', 'Right-Hand Bat', 42, 15000, '{"matches": 350, "runs": 10000, "catches": 300}', 5, 'QUEUED'),
        (v_config_id, 'Rishabh P.', 'WICKET_KEEPERS', 'Left-Hand Bat', 26, 7000, '{"matches": 120, "runs": 3500, "strikeRate": 145}', 6, 'QUEUED'),
        (v_config_id, 'Rashid K.', 'BOWLERS', 'Right-Arm Leg Spin', 25, 9000, '{"matches": 200, "wickets": 280, "economy": 6.2}', 7, 'QUEUED'),
        (v_config_id, 'Surya K.', 'BATSMEN', 'Right-Hand Bat', 33, 8500, '{"matches": 160, "runs": 4500, "strikeRate": 165}', 8, 'QUEUED'),
        (v_config_id, 'Ravindra J.', 'ALL_ROUNDERS', 'Spin All-rounder', 35, 7500, '{"matches": 210, "runs": 4000, "wickets": 210}', 9, 'QUEUED'),
        (v_config_id, 'Trent B.', 'BOWLERS', 'Left-Arm Fast', 34, 7000, '{"matches": 180, "wickets": 220, "economy": 7.0}', 10, 'QUEUED');

END $$;

-- Verify insertion
-- SELECT * FROM auction_config;
-- SELECT * FROM auction_team;
-- SELECT * FROM auction_player;
