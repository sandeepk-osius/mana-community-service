package com.manacommunity.api.constants;

import java.util.List;

/**
 * PermissionConstants — Single source of truth for all permission keys used in the system.
 * These values are stored in the `role_permissions.permission_key` column.
 *
 * <p>Usage: Import statically where needed.</p>
 * <pre>
 *   import static com.manacommunity.api.constants.PermissionConstants.*;
 * </pre>
 */
public final class PermissionConstants {

    private PermissionConstants() {
        // Non-instantiable utility class
    }

    // ──── COMMUNITY FEED ────────────────────────────────────────────
    public static final String VIEW_FEED       = "View Feed";
    public static final String CREATE_POST     = "Create Post";
    public static final String DELETE_POST     = "Delete Post";
    public static final String COMMENT_ON_POST = "Comment on Post";

    // ──── SPORTS ────────────────────────────────────────────────────
    public static final String VIEW_SPORTS        = "View Sports";
    public static final String REGISTER_SPORTS    = "Register Sports";
    public static final String MANAGE_TOURNAMENTS = "Manage Tournaments";
    public static final String BIDDING_INTERFACE  = "Bidding Interface";

    // ──── MARKETPLACE ───────────────────────────────────────────────
    public static final String VIEW_MARKETPLACE = "View Marketplace";
    public static final String CREATE_LISTING   = "Create Listing";
    public static final String DELETE_LISTING   = "Delete Listing";

    // ──── JOBS & REFERRALS ──────────────────────────────────────────
    public static final String VIEW_JOBS  = "View Jobs";
    public static final String CREATE_JOB = "Create Job";
    public static final String APPLY_JOB  = "Apply Job";

    // ──── EVENTS ────────────────────────────────────────────────────
    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";

    // ──── ADMIN DASHBOARD ───────────────────────────────────────────
    public static final String VIEW_ADMIN         = "View Admin";
    public static final String VERIFY_KYC         = "Verify KYC";
    public static final String BULK_UPLOAD        = "Bulk Upload";
    public static final String MANAGE_COMMUNITIES = "Manage Communities";
    public static final String MANAGE_ROLES       = "Manage Roles";

    /**
     * Master list of ALL permission keys in the system.
     * Used by the seeder and can be exposed via API.
     */
    public static final List<String> ALL_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST,
            VIEW_SPORTS, REGISTER_SPORTS, MANAGE_TOURNAMENTS, BIDDING_INTERFACE,
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
            VIEW_JOBS, CREATE_JOB, APPLY_JOB,
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT,
            VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_COMMUNITIES, MANAGE_ROLES
    );

    public static final List<String> ADMIN_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST,
            VIEW_SPORTS, REGISTER_SPORTS, MANAGE_TOURNAMENTS, BIDDING_INTERFACE,
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
            VIEW_JOBS, CREATE_JOB, APPLY_JOB,
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT,
            VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_ROLES
    );


    public static final List<String> SPORTS_ADMIN_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS, REGISTER_SPORTS, MANAGE_TOURNAMENTS, BIDDING_INTERFACE,
            VIEW_MARKETPLACE,
            VIEW_JOBS,
            VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT,
            VIEW_ADMIN
    );



    public static final List<String> MEMBER_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
                        VIEW_SPORTS, REGISTER_SPORTS, BIDDING_INTERFACE,
                        VIEW_MARKETPLACE,
                        VIEW_JOBS, APPLY_JOB,
                        VIEW_EVENTS, REGISTER_EVENT
                        );

    // VENDOR
    public static final List<String> VENDOR_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
                        VIEW_SPORTS,
                        VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
                        VIEW_JOBS, CREATE_JOB,
                        VIEW_EVENTS, REGISTER_EVENT
                        );

    // CASHIER
    public static final List<String> CASHIER_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
                        VIEW_SPORTS,
                        VIEW_MARKETPLACE,
                        VIEW_JOBS,
                        VIEW_EVENTS
                        );

    // STAFF
    public static final List<String> STAFF_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
                        VIEW_SPORTS,
                        VIEW_MARKETPLACE,
                        VIEW_JOBS,
                        VIEW_EVENTS
                        );
}
