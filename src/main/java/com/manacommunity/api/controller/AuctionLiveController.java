package com.manacommunity.api.controller;

import com.manacommunity.api.dto.BidRequest;
import com.manacommunity.api.dto.PlayerWithBidResponse;
import com.manacommunity.api.dto.SoldPlayerRequest;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionBid;
import com.manacommunity.api.model.AuctionPlayer;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuctionService;
import com.manacommunity.api.service.AuctionTeamService;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.service.PermissionCheckService;
import static com.manacommunity.api.constants.PermissionConstants.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/auction/live")
@RequiredArgsConstructor
public class AuctionLiveController {

    private final AuctionService auctionService;
    private final AuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    /** GET current player up for auction with live bid data */
    @GetMapping("/{configId}/current-player")
    public ResponseEntity<PlayerWithBidResponse> getCurrentPlayer(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getCurrentPlayer(configId));
    }

    /** GET pick a random QUEUED player and set them to SELLING */
    @GetMapping("/{configId}/random-player")
    public ResponseEntity<PlayerWithBidResponse> pickRandomPlayer(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.pickRandomPlayer(configId));
    }

    /** POST place a bid (team owner / auctioneer) */
    @PostMapping("/bid")
    public ResponseEntity<AuctionBid> placeBid(
            @Valid @RequestBody BidRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionService.placeBid(req, loggedInUser.getId()));
    }

    /** POST mark player as sold to highest bidder */
    @PostMapping("/sold")
    public ResponseEntity<AuctionPlayer> soldPlayer(
            @Valid @RequestBody SoldPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.soldPlayer(req, loggedInUser.getId()));
    }

    /** POST pass player — moves to rotation queue per unsold rule */
    @PostMapping("/{playerId}/pass")
    public ResponseEntity<AuctionPlayer> passPlayer(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.passPlayer(playerId, loggedInUser.getId()));
    }

    /** GET bid history for a player */
    @GetMapping("/bids/{playerId}")
    public ResponseEntity<List<AuctionBid>> getBidHistory(
            @PathVariable Long playerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getBidHistory(playerId));
    }

    /** GET all teams with budget status */
    @GetMapping("/{configId}/teams")
    public ResponseEntity<List<AuctionTeam>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getTeams(configId));
    }

    /** GET full player pool for an auction */
    @GetMapping("/{configId}/players")
    public ResponseEntity<List<AuctionPlayer>> getPlayers(
            @PathVariable Long configId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_LIVE_AUCTION);
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionService.getPlayers(configId, category, status));
    }
}
