package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * RolePermission Entity
 * Maps user roles (e.g. ADMIN, MEMBER, VENDOR) to dynamic menu-based permission keys.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"permissions", "users"})
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "community_id"}))
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "SUPER_ADMIN"

    @Column(name = "community_id")
    private Long communityId;

    // The core mapping linked directly by role_id to role_permissions.role_id
    @OneToMany(mappedBy = "roleEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<RolePermission> permissions = new HashSet<>();

    // --- NEW: Mapped back to the User table (One-to-Many) ---
    // "mappedBy" tells Hibernate that the 'role' field in the User class owns this relationship.
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<User> users = new HashSet<>();
}
