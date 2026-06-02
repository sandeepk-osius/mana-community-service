package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_community_mapping")
@IdClass(UserCommunityMappingId.class)
@Data
public class UserCommunityMapping {
    @Id
    private String userId;
    @Id
    private String communityId;
    private String role;
    private String joinStatus;
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() { joinedAt = LocalDateTime.now(); }
}

// Composite Key class for the mapping table
@Data
class UserCommunityMappingId implements Serializable {
    private String userId;
    private String communityId;
}
