package com.manacommunity.api.controller;

import com.manacommunity.api.dto.AuctionConfigRequest;
import com.manacommunity.api.dto.AuctionConfigResponse;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionConfig;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuctionCsvService;
import com.manacommunity.api.service.AuctionService;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auction/config")
@RequiredArgsConstructor
public class AuctionConfigController {

    private final AuctionService     auctionService;
    private final AuctionCsvService  csvService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<List<AuctionConfigResponse>> getConfigs(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getConfigsBySportAndCommunity(sportId, communityId)
            .stream().map(c -> auctionService.getConfigResponse(c.getId())).toList());
    }

    /** GET all configs for the user's community across all sports */
    @GetMapping("/all")
    public ResponseEntity<List<AuctionConfigResponse>> getCommunityConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        return ResponseEntity.ok(auctionService.getAllConfigsByCommunity(communityId)
            .stream().map(c -> auctionService.getConfigResponse(c.getId())).toList());
    }

    /** GET check if auction config exists for the logged-in user's community */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkConfigExists(
            @RequestParam Long sportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long communityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        List<AuctionConfig> configs = auctionService.getConfigsBySportAndCommunity(sportId, communityId);
        boolean exists = !configs.isEmpty();
        return ResponseEntity.ok(Map.of(
                "configExists", exists,
                "configCount", configs.size(),
                "communityId", communityId != null ? communityId : 0
        ));
    }

    /** GET single config by ID */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionConfigResponse> getConfig(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getConfigResponse(id));
    }

    /** GET auction stats by config ID */
    @GetMapping("/{id}/stats")
    public ResponseEntity<com.manacommunity.api.dto.AuctionStatsResponse> getStats(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getAuctionStats(id));
    }

    /** POST create new auction config (admin only) */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionConfig> createConfig(
            @Valid @RequestBody AuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionService.createConfig(req, loggedInUser.getId()));
    }

    /** PUT update auction rules dynamically — cannot update when LIVE */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionConfig> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody AuctionConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.updateConfig(id, req));
    }

    /** PUT change auction status: DRAFT→ACTIVE→LIVE→COMPLETED */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionConfig> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.updateStatus(id, status));
    }

    /** POST upload players via CSV / Excel */
    @PostMapping("/{id}/players/upload")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<String> uploadPlayers(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        int count = csvService.uploadPlayersFromFile(id, file);
        return ResponseEntity.ok("Uploaded " + count + " players to auction pool");
    }

    /** POST create single player manually */
    @PostMapping("/{id}/players")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<com.manacommunity.api.model.AuctionPlayer> createPlayer(
            @PathVariable Long id,
            @Valid @RequestBody com.manacommunity.api.dto.AuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionService.createPlayer(id, req));
    }

    /** GET /api/auction/config/{id}/registration-count — get confirmed registration count */
    @GetMapping("/{id}/registration-count")
    public ResponseEntity<Long> getRegistrationCount(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getConfirmedRegistrationCount(id));
    }
}
