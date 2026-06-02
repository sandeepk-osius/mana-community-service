-- Seeding roles table with baseline roles
INSERT INTO roles (name) VALUES ('SUPER_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('SPORTS_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('MEMBER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('VENDOR') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('CASHIER') ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('STAFF') ON CONFLICT (name) DO NOTHING;

-- Seeding role permissions mapping
-- Helper to perform conditional insert since role_permissions table may not have unique constraints on (role, permission_key)
CREATE OR REPLACE FUNCTION seed_role_permission(role_name VARCHAR, perm_key VARCHAR) 
RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM role_permissions 
        WHERE role = UPPER(role_name) AND permission_key = perm_key AND user_id IS NULL
    ) THEN
        INSERT INTO role_permissions (role, permission_key) VALUES (UPPER(role_name), perm_key);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- SUPER_ADMIN and ADMIN Permissions
SELECT seed_role_permission('SUPER_ADMIN', 'View Feed');
SELECT seed_role_permission('SUPER_ADMIN', 'Create Post');
SELECT seed_role_permission('SUPER_ADMIN', 'Delete Post');
SELECT seed_role_permission('SUPER_ADMIN', 'Comment on Post');
SELECT seed_role_permission('SUPER_ADMIN', 'View Sports');
SELECT seed_role_permission('SUPER_ADMIN', 'Register Sports');
SELECT seed_role_permission('SUPER_ADMIN', 'Manage Tournaments');
SELECT seed_role_permission('SUPER_ADMIN', 'Bidding Interface');
SELECT seed_role_permission('SUPER_ADMIN', 'View Marketplace');
SELECT seed_role_permission('SUPER_ADMIN', 'Create Listing');
SELECT seed_role_permission('SUPER_ADMIN', 'Delete Listing');
SELECT seed_role_permission('SUPER_ADMIN', 'View Jobs');
SELECT seed_role_permission('SUPER_ADMIN', 'Create Job');
SELECT seed_role_permission('SUPER_ADMIN', 'Apply Job');
SELECT seed_role_permission('SUPER_ADMIN', 'View Events');
SELECT seed_role_permission('SUPER_ADMIN', 'Create Event');
SELECT seed_role_permission('SUPER_ADMIN', 'Register Event');
SELECT seed_role_permission('SUPER_ADMIN', 'View Admin');
SELECT seed_role_permission('SUPER_ADMIN', 'Verify KYC');
SELECT seed_role_permission('SUPER_ADMIN', 'Bulk Upload');
SELECT seed_role_permission('SUPER_ADMIN', 'Manage Communities');
SELECT seed_role_permission('SUPER_ADMIN', 'Manage Roles');

SELECT seed_role_permission('ADMIN', 'View Feed');
SELECT seed_role_permission('ADMIN', 'Create Post');
SELECT seed_role_permission('ADMIN', 'Delete Post');
SELECT seed_role_permission('ADMIN', 'Comment on Post');
SELECT seed_role_permission('ADMIN', 'View Sports');
SELECT seed_role_permission('ADMIN', 'Register Sports');
SELECT seed_role_permission('ADMIN', 'Manage Tournaments');
SELECT seed_role_permission('ADMIN', 'Bidding Interface');
SELECT seed_role_permission('ADMIN', 'View Marketplace');
SELECT seed_role_permission('ADMIN', 'Create Listing');
SELECT seed_role_permission('ADMIN', 'Delete Listing');
SELECT seed_role_permission('ADMIN', 'View Jobs');
SELECT seed_role_permission('ADMIN', 'Create Job');
SELECT seed_role_permission('ADMIN', 'Apply Job');
SELECT seed_role_permission('ADMIN', 'View Events');
SELECT seed_role_permission('ADMIN', 'Create Event');
SELECT seed_role_permission('ADMIN', 'Register Event');
SELECT seed_role_permission('ADMIN', 'View Admin');
SELECT seed_role_permission('ADMIN', 'Verify KYC');
SELECT seed_role_permission('ADMIN', 'Bulk Upload');
SELECT seed_role_permission('ADMIN', 'Manage Communities');
SELECT seed_role_permission('ADMIN', 'Manage Roles');

-- SPORTS_ADMIN Permissions
SELECT seed_role_permission('SPORTS_ADMIN', 'View Feed');
SELECT seed_role_permission('SPORTS_ADMIN', 'Create Post');
SELECT seed_role_permission('SPORTS_ADMIN', 'Comment on Post');
SELECT seed_role_permission('SPORTS_ADMIN', 'View Sports');
SELECT seed_role_permission('SPORTS_ADMIN', 'Register Sports');
SELECT seed_role_permission('SPORTS_ADMIN', 'Manage Tournaments');
SELECT seed_role_permission('SPORTS_ADMIN', 'Bidding Interface');
SELECT seed_role_permission('SPORTS_ADMIN', 'View Marketplace');
SELECT seed_role_permission('SPORTS_ADMIN', 'View Jobs');
SELECT seed_role_permission('SPORTS_ADMIN', 'View Events');
SELECT seed_role_permission('SPORTS_ADMIN', 'Create Event');
SELECT seed_role_permission('SPORTS_ADMIN', 'Register Event');
SELECT seed_role_permission('SPORTS_ADMIN', 'View Admin');

-- MEMBER Permissions
SELECT seed_role_permission('MEMBER', 'View Feed');
SELECT seed_role_permission('MEMBER', 'Create Post');
SELECT seed_role_permission('MEMBER', 'Comment on Post');
SELECT seed_role_permission('MEMBER', 'View Sports');
SELECT seed_role_permission('MEMBER', 'Register Sports');
SELECT seed_role_permission('MEMBER', 'Bidding Interface');
SELECT seed_role_permission('MEMBER', 'View Marketplace');
SELECT seed_role_permission('MEMBER', 'View Jobs');
SELECT seed_role_permission('MEMBER', 'Apply Job');
SELECT seed_role_permission('MEMBER', 'View Events');
SELECT seed_role_permission('MEMBER', 'Register Event');

-- VENDOR Permissions
SELECT seed_role_permission('VENDOR', 'View Feed');
SELECT seed_role_permission('VENDOR', 'Create Post');
SELECT seed_role_permission('VENDOR', 'Comment on Post');
SELECT seed_role_permission('VENDOR', 'View Sports');
SELECT seed_role_permission('VENDOR', 'View Marketplace');
SELECT seed_role_permission('VENDOR', 'Create Listing');
SELECT seed_role_permission('VENDOR', 'Delete Listing');
SELECT seed_role_permission('VENDOR', 'View Jobs');
SELECT seed_role_permission('VENDOR', 'Create Job');
SELECT seed_role_permission('VENDOR', 'View Events');
SELECT seed_role_permission('VENDOR', 'Register Event');

-- CASHIER and STAFF Permissions
SELECT seed_role_permission('CASHIER', 'View Feed');
SELECT seed_role_permission('CASHIER', 'Comment on Post');
SELECT seed_role_permission('CASHIER', 'View Sports');
SELECT seed_role_permission('CASHIER', 'View Marketplace');
SELECT seed_role_permission('CASHIER', 'View Jobs');
SELECT seed_role_permission('CASHIER', 'View Events');

SELECT seed_role_permission('STAFF', 'View Feed');
SELECT seed_role_permission('STAFF', 'Comment on Post');
SELECT seed_role_permission('STAFF', 'View Sports');
SELECT seed_role_permission('STAFF', 'View Marketplace');
SELECT seed_role_permission('STAFF', 'View Jobs');
SELECT seed_role_permission('STAFF', 'View Events');

DROP FUNCTION seed_role_permission(role_name VARCHAR, perm_key VARCHAR);
