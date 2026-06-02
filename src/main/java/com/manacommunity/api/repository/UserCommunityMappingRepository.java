package com.manacommunity.api.repository;

import com.manacommunity.api.model.UserCommunityMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCommunityMappingRepository extends JpaRepository<UserCommunityMapping, String> {
    // Custom query methods can be added here (e.g., find communities by userId)
}
