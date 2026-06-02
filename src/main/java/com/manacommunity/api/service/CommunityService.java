package com.manacommunity.api.service;

import com.manacommunity.api.response.CommunityResponse;

import java.util.List;

public interface CommunityService {

    /** Returns all active communities for the signup dropdown. */
    List<CommunityResponse> getAllCommunities();

    /** Returns communities filtered by type (APARTMENT, COLLEGE, etc.). */
    List<CommunityResponse> getCommunitiesByType(String type);

    /** Creates a new community. */
    CommunityResponse createCommunity(CommunityResponse request);
}
