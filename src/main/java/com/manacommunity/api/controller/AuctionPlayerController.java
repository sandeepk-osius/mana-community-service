package com.manacommunity.api.controller;

import com.manacommunity.api.dto.AuctionPlayerRequest;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.AuctionPlayer;
import com.manacommunity.api.model.AuctionConfig;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.AuctionPlayerService;
import com.manacommunity.api.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction/players")
@RequiredArgsConstructor
public class AuctionPlayerController {

    private final AuctionPlayerService auctionPlayerService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','AUCTION_ADMIN','SPORTS_ADMIN','COMMUNITY_ADMIN')")
    public ResponseEntity<AuctionPlayer> createPlayer(
            @RequestBody AuctionPlayerRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        AuctionPlayer player = AuctionPlayer.builder()
                .config(AuctionConfig.builder().id(req.getConfigId()).build())
                .user(AppUser.builder().id(req.getUserId()).build())
                .playerName(req.getPlayerName())
                .category(req.getCategory())
                .playerRole(req.getPlayerRole())
                .age(req.getAge())
                .basePrice(req.getBasePrice())
                //.statsJson(req.getStatsJson())
                //.queueOrder(req.getQueueOrder())
                .status(AuctionPlayer.PlayerStatus.QUEUED)
                .build();
        AuctionPlayer saved = auctionPlayerService.savePlayer(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}