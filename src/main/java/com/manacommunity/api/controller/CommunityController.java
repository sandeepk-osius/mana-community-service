package com.manacommunity.api.controller;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.response.CommunityResponse;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.CommunityService;
import com.manacommunity.api.service.LoggedInUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Community endpoints.
 * GET is public (signup dropdown). POST requires authentication.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private LoggedInUserService loggedInUserService;

    /**
     * GET /api/communities
     * Returns all communities for the signup dropdown.
     * Optional ?type= filter (APARTMENT | COLLEGE | SCHOOL | OFFICE)
     *
     * Example:
     *   GET /api/communities          → all communities
     *   GET /api/communities?type=APARTMENT → only apartments
     */
    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getCommunities(
            @RequestParam(required = false) String type) {

        List<CommunityResponse> result = (type != null && !type.isBlank())
                ? communityService.getCommunitiesByType(type)
                : communityService.getAllCommunities();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/communities
     * Creates a new community. Requires authentication.
     */
    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @RequestBody CommunityResponse request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(communityService.createCommunity(request));
    }
}
