package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
public class User {
    @Id
    private String id;
    private String cognitoSub; // Link to AWS Cognito (or local mock ID)
    private String fullName;
    private String email;
    private String phoneNumber;
    private String aadharNumberMasked;
    private String kycStatus;
    private String profileImageUrl;
    private String passwordHash; // Used for local Spring Boot authentication testing

    // --- THE MAPPING TO ROLE ---
    // Many users can have the same Role.
    // FetchType.EAGER ensures that when you load a user, their role (and permissions) are loaded immediately.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCognitoSub() {
        return cognitoSub;
    }

    public void setCognitoSub(String cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAadharNumberMasked() {
        return aadharNumberMasked;
    }

    public void setAadharNumberMasked(String aadharNumberMasked) {
        this.aadharNumberMasked = aadharNumberMasked;
    }

    public String getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(String kycStatus) {
        this.kycStatus = kycStatus;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
