package com.manacommunity.api.service.impl;

import com.manacommunity.api.response.AuthResponse;
import com.manacommunity.api.dto.KycRequest;
import com.manacommunity.api.dto.LoginRequest;
import com.manacommunity.api.dto.RegisterRequest;
import com.manacommunity.api.exception.*;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Role;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.RoleRepository;
import com.manacommunity.api.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private CommunityRepository communityRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {

        // 1. Verify Community Invite Code
        Community community = communityRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new InvalidInviteCodeException(request.getInviteCode()));

        // 2. Duplicate email / phone check (both are UNIQUE in DB)
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("User", "email", request.getEmail());

        if (userRepository.existsByPhone(request.getPhone()))
            throw new DuplicateResourceException("User", "phone", request.getPhone());

        // 3. Mock Aadhaar masking
        String maskedAadhar = maskAadharNumber(request.getAadharNumber());

        // 4. Build and save AppUser
        AppUser user = new AppUser();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setGovtIdNumber(maskedAadhar);
        user.setGovtIdType("AADHAAR");
        user.setKycStatus("VERIFIED");
        user.setRole("MEMBER"); // Standard role for new users
        Role memberRole;
        if (community != null) {
            memberRole = roleRepository.findByNameIgnoreCaseAndCommunityId("MEMBER", community.getId())
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("MEMBER")
                            .communityId(community.getId())
                            .build()));
        } else {
            memberRole = roleRepository.findByNameIgnoreCaseAndCommunityIdIsNull("MEMBER")
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .name("MEMBER")
                            .build()));
        }
        user.setRoleEntity(memberRole);
        user.setIsActive(true);
        user.setCommunity(community);
        user.setFlatNo(request.getFlatNo());
        user.setBlock(request.getBlock());

        AppUser saved = userRepository.save(user);
        return new AuthResponse(
                String.valueOf(saved.getId()), 
                "Registration & KYC successful!",
                "mock-token-" + saved.getId(),
                saved.getFullName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getCommunity() != null ? saved.getCommunity().getId() : null,
                saved.getDateOfBirth()
        );
    }

    @Override
    public AuthResponse loginUser(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new ManaCommunityException("Invalid password. Please try again.",
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");

        return new AuthResponse(
                String.valueOf(user.getId()), 
                "Login successful!", 
                "mock-token-" + user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCommunity() != null ? user.getCommunity().getId() : null,
                user.getDateOfBirth()
        );
    }

    @Override
    public boolean submitKyc(Long userId, KycRequest req) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (req.getConsentGiven() == null || !req.getConsentGiven())
            throw new ManaCommunityException("KYC consent is required to proceed.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "KYC_CONSENT_MISSING");

        user.setGovtIdType(req.getGovtIdType().name());
        user.setGovtIdNumber(req.getGovtIdNumber()); // encrypt before storing in production
        user.setKycStatus("PENDING");
        userRepository.save(user);
        return true;
    }

    private String maskAadharNumber(String aadhar) {
        if (aadhar == null || aadhar.length() < 4)
            return "INVALID_ID";
        return "XXXX-XXXX-" + aadhar.substring(aadhar.length() - 4);
    }
}
