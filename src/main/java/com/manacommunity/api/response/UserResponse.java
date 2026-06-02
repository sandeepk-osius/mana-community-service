package com.manacommunity.api.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String kycStatus;
    private String profilePicUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String flatNo;
    private String block;
    private Long communityId;
    private Boolean isActive;
    private java.util.List<String> permissions;
}
