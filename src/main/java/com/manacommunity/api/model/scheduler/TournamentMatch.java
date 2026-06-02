package com.manacommunity.api.model.scheduler;

import com.manacommunity.api.model.AuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "tournament_match")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TournamentMatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private TournamentConfig config;

    // ── Bracket / group context ───────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;   // null for knockout rounds

    @Enumerated(EnumType.STRING)
    private MatchRound round;

    private Integer roundNumber;     // e.g. 1 for QF, 2 for SF, 3 for F
    private Integer matchNumber;     // match 1, 2, 3 in the round
    private Integer bracketSlot;     // position in bracket visualization

    // ── Teams ─────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private AuctionTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private AuctionTeam teamB;

    // For bracket: which match does the winner come from?
    private Long winnerFeedFromMatchA;   // match id
    private Long winnerFeedFromMatchB;   // match id

    // Where does the winner/loser of THIS match go?
    private Long winnerAdvancesToMatchId;
    private Long loserSentToMatchId;     // for double elimination

    // ── Schedule ──────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private Integer durationMinutes;
    private String  venueName;
    private String  courtNumber;

    // ── Result ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private String  scoreTeamA;    // flexible: "45 runs", "21-18,21-15"
    private String  scoreTeamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private AuctionTeam winner;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String  matchNotes;

    // ── Swiss round ───────────────────────────────────────────────
    private Integer swissRoundNumber;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
