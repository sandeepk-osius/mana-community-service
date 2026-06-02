package com.manacommunity.api.controller;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.model.RolePermission;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.RolePermissionRepository;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*") // TODO: restrict in production
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionRepository rolePermissionRepo;
    private final LoggedInUserService loggedInUserService;
    private final AppUserRepository appUserRepository;
    private final com.manacommunity.api.service.RoleService roleService;

    /**
     * GET /api/roles/permissions
     * Returns all roles mapped to their active permission keys.
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, List<String>>> getAllRolePermissions(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = null;
        if (principal != null) {
            try {
                loggedInUser = loggedInUserService.resolve(principal);
            } catch (Exception ignored) {}
        }
        
        Long targetCommunityId = null;
        if (loggedInUser != null && !"SUPER_ADMIN".equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }

        List<RolePermission> all = rolePermissionRepo.findAll();
        final Long finalCommId = targetCommunityId;

        Map<String, List<String>> response = all.stream()
                .filter(rp -> rp.getUser() == null)
                .filter(rp -> {
                    Role roleEntity = rp.getRoleEntity();
                    if (roleEntity == null) {
                        return true;
                    }
                    if (finalCommId != null) {
                        return roleEntity.getCommunityId() == null || roleEntity.getCommunityId().equals(finalCommId);
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(
                        rp -> rp.getRole().toUpperCase(),
                        Collectors.mapping(RolePermission::getPermissionKey, Collectors.toList())
                ));
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/roles/{role}/permissions
     * Overwrites the permission keys for a given role.
     */
    @PutMapping("/{role}/permissions")
    @Transactional
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable String role,
            @RequestBody List<String> permissions,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        AppUser targetUser;
        if (userId != null) {
            targetUser = appUserRepository.findById(userId)
                    .orElseThrow(() -> new com.manacommunity.api.exception.ResourceNotFoundException("User", userId));
        } else {
            targetUser = loggedInUserService.resolve(principal);
        }
        
        rolePermissionRepo.deleteByRoleIgnoreCaseAndUserId(role, targetUser.getId());
        rolePermissionRepo.flush();
        
        AppUser finalTargetUser = targetUser;
        com.manacommunity.api.model.Role userRoleEntity = finalTargetUser.getRoleEntity();
        List<RolePermission> entities = permissions.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .distinct()
                .map(p -> RolePermission.builder()
                        .role(role.toUpperCase())
                        .roleEntity(userRoleEntity)
                        .permissionKey(p)
                        .user(finalTargetUser)
                        .build())
                .toList();
                
        rolePermissionRepo.saveAll(entities);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/roles
     * Returns a list of all created roles.
     */
    @GetMapping
    public ResponseEntity<List<com.manacommunity.api.model.Role>> getRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * POST /api/roles
     * Creates a new role in the database.
     */
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody Map<String, String> body) {
        String roleName = body.get("name");
        try {
            com.manacommunity.api.model.Role saved = roleService.createRole(roleName);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
