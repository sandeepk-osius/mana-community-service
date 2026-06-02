package com.manacommunity.api.repository;

import com.manacommunity.api.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    
    List<RolePermission> findByRoleIgnoreCase(String role);
    
    void deleteByRoleIgnoreCase(String role);
    
    void deleteByRoleIgnoreCaseAndUserId(String role, Long userId);
    
    List<RolePermission> findByRoleIn(List<String> roles);
    
    List<RolePermission> findByUserId(Long userId);
    
    void deleteByUserId(Long userId);
}
