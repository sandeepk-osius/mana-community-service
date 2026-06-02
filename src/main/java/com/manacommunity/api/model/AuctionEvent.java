package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BUG FIX: AuctionEvent was an empty stub class.
 * Now fully mapped to the auction_event table.
 * - AuctionServiceImpl reads auction.getBidIncrement() — field must exist.
 */
@Entity
@Table(name = "auction_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sports_event_id", nullable = false)
    private SportsEvent sportsEvent;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String status = "UPCOMING"; // UPCOMING, LIVE, COMPLETED

    @Column(name = "budget_per_team", nullable = false)
    private Integer budgetPerTeam = 5000;

    @Column(name = "bid_increment", nullable = false)
    private Integer bidIncrement = 50;

    @Column(name = "bid_timer_secs", nullable = false)
    private Integer bidTimerSecs = 30;

    @Column(name = "rtm_enabled", nullable = false)
    private Boolean rtmEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
