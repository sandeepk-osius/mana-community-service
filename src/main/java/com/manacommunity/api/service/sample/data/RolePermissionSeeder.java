package com.manacommunity.api.service.sample.data;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.*;

/**
 * RolePermissionSeeder — Handles seeding baseline roles and their respective permissions.
 * All permission keys are sourced from {@link com.manacommunity.api.constants.PermissionConstants}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionSeeder {

    private final RoleRepository roleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final AppUserRepository appUserRepo;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding role permissions...");

        // First, ensure all roles exist in the roles table (global/system roles)
        List<String> rolesToSeed = List.of("SUPER_ADMIN", "ADMIN", "SPORTS_ADMIN", "MEMBER", "VENDOR", "CASHIER", "STAFF");
        for (String roleName : rolesToSeed) {
            if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(roleName)) {
                roleRepo.save(Role.builder().name(roleName.toUpperCase()).build());
            }
        }

        // SUPER_ADMIN and ADMIN get everything
        saveRolePermissions("SUPER_ADMIN", ALL_PERMISSIONS);
        saveRolePermissions("ADMIN", ADMIN_PERMISSIONS);

        // SPORTS_ADMIN
        saveRolePermissions("SPORTS_ADMIN", SPORTS_ADMIN_PERMISSIONS);

        // MEMBER
        saveRolePermissions("MEMBER", MEMBER_PERMISSIONS);

        // VENDOR
        saveRolePermissions("VENDOR", VENDOR_PERMISSIONS);

        // CASHIER
        saveRolePermissions("CASHIER", CASHIER_PERMISSIONS);

        // STAFF
        saveRolePermissions("STAFF", STAFF_PERMISSIONS);

        log.info("✓ Role permissions seeded successfully");
    }


    @Transactional
    public void seed() {
        log.info("Seeding role permissions...");

        // First, ensure all roles exist in the roles table (global/system roles)
        List<String> rolesToSeed = List.of("SUPER_ADMIN", "ADMIN", "SPORTS_ADMIN", "MEMBER", "VENDOR", "CASHIER", "STAFF");
        for (String roleName : rolesToSeed) {
            if (!roleRepo.existsByNameIgnoreCaseAndCommunityIdIsNull(roleName)) {
                roleRepo.save(Role.builder().name(roleName.toUpperCase()).build());
            }
        }

        // SUPER_ADMIN and ADMIN get everything
        saveRolePermissions("SUPER_ADMIN", ALL_PERMISSIONS);
        saveRolePermissions("ADMIN", ADMIN_PERMISSIONS);

        // SPORTS_ADMIN
        saveRolePermissions("SPORTS_ADMIN", SPORTS_ADMIN_PERMISSIONS);

        // MEMBER
        saveRolePermissions("MEMBER", MEMBER_PERMISSIONS);

        // VENDOR
        saveRolePermissions("VENDOR", VENDOR_PERMISSIONS);

        // CASHIER
        saveRolePermissions("CASHIER", CASHIER_PERMISSIONS);

        // STAFF
        saveRolePermissions("STAFF", STAFF_PERMISSIONS);

        log.info("✓ Role permissions seeded successfully");
    }

    private void saveRolePermissions(String role, List<String> permissions) {
        Role roleEntity = roleRepo.findByNameIgnoreCaseAndCommunityIdIsNull(role)
                .orElseThrow(() -> new IllegalStateException("Global Role " + role + " not found"));
        // Fetch existing permissions once per role instead of calling findAll() per permission
        List<RolePermission> existingPerms = rolePermissionRepo.findByRoleIgnoreCase(role);
        for (String perm : permissions) {
            boolean exists = existingPerms.stream()
                    .anyMatch(rp -> rp.getPermissionKey().equalsIgnoreCase(perm)
                            && rp.getUser() == null
                            && rp.getRoleEntity() != null
                            && rp.getRoleEntity().getId().equals(roleEntity.getId()));
            if (!exists) {
                RolePermission rp = RolePermission.builder()
                        .role(role.toUpperCase())
                        .roleEntity(roleEntity)
                        .permissionKey(perm)
                        .build();
                rolePermissionRepo.save(rp);
            }
        }
    }

    @Transactional
    public void seedUserPermissions() {
        log.info("Seeding user-specific permissions for Sunil...");
        appUserRepo.findByEmail("sunil@gmail.com").ifPresent(sunil -> {
            Role adminRole = sunil.getRoleEntity();
            if (adminRole != null) {
                List<RolePermission> existingPerms = rolePermissionRepo.findByUserId(sunil.getId());
                for (String perm : ADMIN_PERMISSIONS) {
                    boolean exists = existingPerms.stream()
                            .anyMatch(rp -> rp.getPermissionKey().equalsIgnoreCase(perm)
                                    && "ADMIN".equalsIgnoreCase(rp.getRole())
                                    && rp.getRoleEntity() != null
                                    && rp.getRoleEntity().getId().equals(adminRole.getId()));
                    if (!exists) {
                        RolePermission rp = RolePermission.builder()
                                .role("ADMIN")
                                .roleEntity(adminRole)
                                .permissionKey(perm)
                                .user(sunil)
                                .build();
                        rolePermissionRepo.save(rp);
                    }
                }
                log.info("✓ User-specific permissions seeded for Sunil (ADMIN)");
            }
        });
    }
}
