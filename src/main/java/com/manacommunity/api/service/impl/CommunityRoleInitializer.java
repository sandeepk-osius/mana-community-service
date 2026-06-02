package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityRoleInitializer {

    private final RoleRepository roleRepo;

    private static final List<String> ALL_PERMISSIONS = List.of(
            "View Feed", "Create Post", "Delete Post", "Comment on Post",
            "View Sports", "Register Sports", "Manage Tournaments", "Bidding Interface",
            "View Marketplace", "Create Listing", "Delete Listing",
            "View Jobs", "Create Job", "Apply Job",
            "View Events", "Create Event", "Register Event",
            "View Admin", "Verify KYC", "Bulk Upload", "Manage Communities", "Manage Roles"
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS_MAP = Map.of(
            "ADMIN", ALL_PERMISSIONS,
            "SPORTS_ADMIN", List.of(
                    "View Feed", "Create Post", "Comment on Post",
                    "View Sports", "Register Sports", "Manage Tournaments", "Bidding Interface",
                    "View Marketplace", "View Jobs", "View Events", "Create Event", "Register Event",
                    "View Admin"
            ),
            "MEMBER", List.of(
                    "View Feed", "Create Post", "Comment on Post",
                    "View Sports", "Register Sports", "Bidding Interface",
                    "View Marketplace", "View Jobs", "Apply Job", "View Events", "Register Event"
            ),
            "VENDOR", List.of(
                    "View Feed", "Create Post", "Comment on Post",
                    "View Sports", "View Marketplace", "Create Listing", "Delete Listing",
                    "View Jobs", "Create Job", "View Events", "Register Event"
            ),
            "CASHIER", List.of(
                    "View Feed", "Comment on Post", "View Sports", "View Marketplace", "View Jobs", "View Events"
            ),
            "STAFF", List.of(
                    "View Feed", "Comment on Post", "View Sports", "View Marketplace", "View Jobs", "View Events"
            )
    );

    @Transactional
    public void initializeCommunityRoles(Community community) {
        if (community == null || community.getId() == null) {
            log.warn("Cannot initialize roles: community or community ID is null");
            return;
        }

        log.info("Initializing baseline roles for community: {} (ID: {})", community.getName(), community.getId());
        Long communityId = community.getId();

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSIONS_MAP.entrySet()) {
            String roleName = entry.getKey();
            List<String> perms = entry.getValue();

            // Check if this community role already exists
            boolean exists = roleRepo.existsByNameIgnoreCaseAndCommunityId(roleName, communityId);
            if (!exists) {
                Role role = Role.builder()
                        .name(roleName)
                        .communityId(communityId)
                        .permissions(new HashSet<>())
                        .build();

                for (String p : perms) {
                    RolePermission rp = RolePermission.builder()
                            .role(roleName)
                            .permissionKey(p)
                            .roleEntity(role)
                            .build();
                    role.getPermissions().add(rp);
                }

                roleRepo.save(role);
                log.info("Created community-scoped role: {} for community ID: {}", roleName, communityId);
            }
        }
    }
}
