package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.response.CommunityResponse;
import com.manacommunity.api.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CommunityRoleInitializer communityRoleInitializer;

    @Override
    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityResponse> getCommunitiesByType(String type) {
        return communityRepository.findByTypeIgnoreCase(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CommunityResponse createCommunity(CommunityResponse request) {
        Community community = toEntity(request);
        Community saved = communityRepository.save(community);
        communityRoleInitializer.initializeCommunityRoles(saved);
        return toResponse(saved);
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private Community toEntity(CommunityResponse r) {
        return Community.builder()
                .name(r.getName())
                .type(r.getType())
                .city(r.getCity())
                .state(r.getState())
                .area(r.getArea())
                .subtype(r.getSubtype())
                .inviteCode(r.getInviteCode())
                .build();
    }

    private CommunityResponse toResponse(Community c) {
        return new CommunityResponse(
                c.getId(),
                c.getName(),
                c.getType(),
                c.getCity(),
                c.getState(),
                c.getArea(),
                c.getSubtype(),
                c.getInviteCode()
        );
    }
}
