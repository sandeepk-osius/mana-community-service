package com.manacommunity.api.controller;

import com.manacommunity.api.dto.AuctionTeamRequest;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuctionTeamService;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auction/teams")
@RequiredArgsConstructor
public class AuctionTeamController {

    private final AuctionTeamService auctionTeamService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{configId}")
    public ResponseEntity<List<AuctionTeam>> getTeams(
            @PathVariable Long configId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(auctionTeamService.getTeams(configId));
    }

    @GetMapping("/nominated/{eventId}")
    public ResponseEntity<List<AuctionTeam>> getNominatedCaptains(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(auctionTeamService.getNominatedCaptains(eventId));
    }

    @GetMapping("/captain/mine")
    public ResponseEntity<List<AuctionTeam>> getMyCaptainRegistrations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionTeam> createTeam(
            @RequestBody AuctionTeamRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(auctionTeamService.createTeam(req, loggedInUser.getId()));
    }

    @PutMapping("/{teamId}/confirm-captain")
    public ResponseEntity<AuctionTeam> confirmCaptain(
            @PathVariable Long teamId,
            @RequestParam boolean confirm) {
        return ResponseEntity.ok(auctionTeamService.confirmCaptain(teamId, confirm));
    }

    @PostMapping("/nominate")
    public ResponseEntity<AuctionTeam> nominateCaptain(
            @RequestParam Long eventId,
            @RequestParam boolean nominate,
            @RequestParam(required = false) String teamName,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.nominateCaptain(eventId, loggedInUser.getId(), nominate, teamName));
    }

    @GetMapping("/captain/all")
    public ResponseEntity<List<AuctionTeam>> getAllCaptainNominations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(auctionTeamService.getCaptainRegistration(loggedInUser.getId()));
    }
}
