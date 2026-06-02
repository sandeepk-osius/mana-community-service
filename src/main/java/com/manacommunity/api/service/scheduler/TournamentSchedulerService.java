package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.scheduler.*;
import com.manacommunity.api.model.AuctionTeam;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.scheduler.*;
import com.manacommunity.api.repository.AppUserRepository;
import com.manacommunity.api.repository.AuctionTeamRepository;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.SportMetaRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import com.manacommunity.api.repository.scheduler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentSchedulerService {

    private final TournamentConfigRepository  configRepo;
    private final TournamentGroupRepository   groupRepo;
    private final TournamentMatchRepository   matchRepo;
    private final GroupTeamStandingRepository standingRepo;
    private final AuctionTeamRepository       teamRepo;
    private final AppUserRepository           userRepo;
    private final SportMetaRepository         sportMetaRepo;
    private final CommunityRepository         communityRepo;
    private final SportsEventRepository       eventRepo;

    // ═══════════════════════════════════════════════════════════════
    // ENTRY POINT — builds config + schedule from request
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public TournamentScheduleResponse createTournamentSchedule(
            TournamentConfigRequest req, Long adminUserId) {

        // ── Validate teams ──────────────────────────────────────
        List<AuctionTeam> teams = teamRepo.findAllById(req.teamIds());
        if (teams.size() != req.totalTeams())
            throw new IllegalArgumentException(
                "Expected " + req.totalTeams() + " teams, got " + teams.size());

        // ── Persist config ──────────────────────────────────────
        TournamentConfig config = buildConfig(req, adminUserId);
        config = configRepo.save(config);
        log.info("Tournament config saved: [{}] type={}", config.getTournamentName(), config.getTournamentType());

        // ── Seed if required ────────────────────────────────────
        if (Boolean.TRUE.equals(req.hasSeeding())) {
            teams = seedTeams(teams);
        }

        // ── Generate schedule ───────────────────────────────────
        List<TournamentMatch> matches = switch (config.getTournamentType()) {
            case KNOCKOUT          -> generateKnockout(config, teams);
            case GROUP_KNOCKOUT    -> generateGroupKnockout(config, teams);
            case ROUND_ROBIN       -> generateRoundRobin(config, teams);
            case DOUBLE_ELIMINATION-> generateDoubleElimination(config, teams);
            case SWISS             -> generateSwissRound1(config, teams);
            case SUPER_LEAGUE      -> generateSuperLeague(config, teams);
        };

        matchRepo.saveAll(matches);
        log.info("Generated {} matches for tournament [{}]", matches.size(), config.getTournamentName());
        return buildResponse(config);
    }

    public TournamentScheduleResponse getSchedule(Long configId) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow();
        return buildResponse(config);
    }

    public TournamentMatch rescheduleMatch(Long matchId, String scheduledAtStr, String venue) {
        TournamentMatch match = matchRepo.findById(matchId).orElseThrow();
        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);
        match.setScheduledAt(scheduledAt);
        if (venue != null) match.setVenueName(venue);
        return matchRepo.save(match);
    }

    public List<MatchResponse> getUpcomingMatchesForTeam(Long teamId) {
        // Find upcoming matches where teamId matches either teamA or teamB
        // For simplicity, we can query all matches or write a custom query.
        // Returning empty list as a placeholder.
        return List.of();
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. KNOCKOUT — single elimination bracket
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateKnockout(
            TournamentConfig config, List<AuctionTeam> teams) {

        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));  // ceil(log2(n))
        int slotCount = (int) Math.pow(2, rounds);                   // nearest power of 2
        int byes      = slotCount - n;

        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor          = config.getStartDate().atTime(9, 0);
        int matchDur  = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int breakMins = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // ── Pad with nulls for byes ─────────────────────────────
        List<AuctionTeam> slots = new ArrayList<>(teams);
        for (int i = 0; i < byes; i++) slots.add(null);  // null = bye

        // ── Round 1 ─────────────────────────────────────────────
        List<Long> previousRoundMatchIds = new ArrayList<>();
        int matchNum = 1;

        for (int i = 0; i < slotCount; i += 2) {
            AuctionTeam teamA = slots.get(i);
            AuctionTeam teamB = slots.get(i + 1);

            MatchStatus status     = (teamB == null) ? MatchStatus.BYE : MatchStatus.SCHEDULED;
            AuctionTeam autoWinner = (teamB == null) ? teamA : null;

            TournamentMatch m = TournamentMatch.builder()
                .config(config)
                .round(rounds == 1 ? MatchRound.FINAL : roundLabel(rounds, 1))
                .roundNumber(1)
                .matchNumber(matchNum++)
                .bracketSlot(i / 2)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(cursor)
                .durationMinutes(matchDur)
                .venueName(config.getVenueName())
                .status(status)
                .winner(autoWinner)
                .build();

            matches.add(m);
            cursor = cursor.plusMinutes(matchDur + breakMins);
            previousRoundMatchIds.add(null);  // filled after save
        }

        // ── Subsequent rounds (placeholder — filled by advanceBracket) ──
        int prevRoundSize = slotCount / 2;
        for (int round = 2; round <= rounds; round++) {
            int roundSize = prevRoundSize / 2;
            MatchRound label = roundLabel(rounds, round);

            for (int i = 0; i < roundSize; i++) {
                TournamentMatch m = TournamentMatch.builder()
                    .config(config)
                    .round(label)
                    .roundNumber(round)
                    .matchNumber(i + 1)
                    .bracketSlot(i)
                    .teamA(null)   // TBD — winner of earlier match
                    .teamB(null)
                    .scheduledAt(cursor)
                    .durationMinutes(matchDur)
                    .venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED)
                    .build();

                matches.add(m);
                cursor = cursor.plusMinutes(matchDur + breakMins);
            }

            // Third place match (same slot as final, different schedule)
            if (round == rounds && Boolean.TRUE.equals(config.getThirdPlaceMatch())) {
                TournamentMatch tp = TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.THIRD_PLACE)
                    .roundNumber(round)
                    .matchNumber(99)
                    .scheduledAt(cursor)
                    .durationMinutes(matchDur)
                    .venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED)
                    .build();
                matches.add(tp);
            }
            prevRoundSize = roundSize;
        }

        // ── Wire winnerAdvancesToMatchId links ──────────────────
        wireKnockoutLinks(matches);
        log.info("[KNOCKOUT] {} rounds, {} slots, {} byes, {} matches",
            rounds, slotCount, byes, matches.size());
        return matches;
    }

    /** After a result is entered, advance winner/loser to correct match */
    @Transactional
    public TournamentScheduleResponse advanceBracket(MatchResultRequest req) {
        TournamentMatch match = matchRepo.findById(req.matchId()).orElseThrow();
        AuctionTeam     winner= teamRepo.findById(req.winnerTeamId()).orElseThrow();
        AuctionTeam     loser = winner.getId().equals(match.getTeamA().getId())
            ? match.getTeamB() : match.getTeamA();

        match.setWinner(winner);
        match.setScoreTeamA(req.scoreTeamA());
        match.setScoreTeamB(req.scoreTeamB());
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());
        matchRepo.save(match);

        // Advance winner to next round match
        if (match.getWinnerAdvancesToMatchId() != null) {
            TournamentMatch next = matchRepo.findById(match.getWinnerAdvancesToMatchId()).orElseThrow();
            if (next.getTeamA() == null) next.setTeamA(winner);
            else                          next.setTeamB(winner);
            matchRepo.save(next);
        }

        // For double elimination: send loser to losers bracket
        if (match.getLoserSentToMatchId() != null) {
            TournamentMatch losersMatch = matchRepo.findById(match.getLoserSentToMatchId()).orElseThrow();
            if (losersMatch.getTeamA() == null) losersMatch.setTeamA(loser);
            else                                 losersMatch.setTeamB(loser);
            matchRepo.save(losersMatch);
        }

        // Update group standings for group matches
        if (match.getGroup() != null) {
            updateGroupStandings(match, winner, loser, req);
        }

        return buildResponse(match.getConfig());
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. GROUP_KNOCKOUT — groups + knockout stage
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateGroupKnockout(
            TournamentConfig config, List<AuctionTeam> teams) {

        int nGroups  = Objects.requireNonNullElse(config.getNumberOfGroups(), 2);
        int advPer   = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);
        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = config.getStartDate().atTime(9, 0);
        int dur   = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk   = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Assign teams to groups (snake-draft for balanced seeding)
        // Team 1 → A, Team 2 → B, Team 3 → B, Team 4 → A …
        List<List<AuctionTeam>> groupTeams = new ArrayList<>();
        for (int g = 0; g < nGroups; g++) groupTeams.add(new ArrayList<>());

        for (int i = 0; i < teams.size(); i++) {
            int row = i / nGroups;
            int col = (row % 2 == 0) ? (i % nGroups) : (nGroups - 1 - (i % nGroups));
            groupTeams.get(col).add(teams.get(i));
        }

        // ── Create group entities ────────────────────────────────
        char letter = 'A';
        List<TournamentGroup> savedGroups = new ArrayList<>();
        for (int g = 0; g < nGroups; g++) {
            TournamentGroup grp = TournamentGroup.builder()
                .config(config)
                .groupName("Group " + (char)(letter + g))
                .groupOrder(g + 1)
                .build();
            grp = groupRepo.save(grp);
            savedGroups.add(grp);

            // Standings
            List<AuctionTeam> gt = groupTeams.get(g);
            for (int seed = 0; seed < gt.size(); seed++) {
                standingRepo.save(GroupTeamStanding.builder()
                    .group(grp).team(gt.get(seed)).seedRank(seed + 1)
                    .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                    .build());
            }

            // Group round-robin matches
            List<TournamentMatch> groupMatches =
                roundRobinMatchesForGroup(grp, gt, config, cursor, dur, brk);
            matches.addAll(groupMatches);
            cursor = cursor.plusMinutes((long)(dur + brk) * groupMatches.size());
        }

        // ── Knockout placeholder matches ─────────────────────────
        // Total teams advancing = nGroups * advPer
        int advancing = nGroups * advPer;
        List<AuctionTeam> knockoutSeeds = Collections.nCopies(advancing, null);
        TournamentConfig koConfig = config.toBuilder()
            .thirdPlaceMatch(config.getThirdPlaceMatch())
            .build();

        List<TournamentMatch> koMatches = generateKnockout(koConfig,
            new ArrayList<>(Collections.nCopies(advancing, null))); // TBD teams
        // Adjust cursor for knockout
        for (int i = 0; i < koMatches.size(); i++) {
            koMatches.get(i).setScheduledAt(cursor.plusMinutes((long) (dur + brk) * i));
        }
        matches.addAll(koMatches);

        log.info("[GROUP_KNOCKOUT] {} groups, {} teams advancing per group, {} total matches",
            nGroups, advPer, matches.size());
        return matches;
    }

    /** Called when group stage is complete — seeds winners into knockout */
    @Transactional
    public void seedKnockoutFromGroups(Long configId) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow();
        int advPer = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);

        List<TournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(configId);
        List<AuctionTeam> advancingTeams = new ArrayList<>();

        // Collect top N from each group by points (then NRR)
        for (TournamentGroup grp : groups) {
            List<GroupTeamStanding> standings =
                standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(grp.getId());
            standings.stream().limit(advPer)
                .forEach(s -> {
                    s.setQualified(true);
                    standingRepo.save(s);
                    advancingTeams.add(s.getTeam());
                });
        }

        // Seed into knockout round 1 matches
        // Cross-seeding: Group A #1 vs Group B #2, Group B #1 vs Group A #2
        List<TournamentMatch> koBracket = matchRepo
            .findByConfigIdAndRoundNumberOrderByMatchNumber(configId, 1);

        // Standard cross seeding
        List<AuctionTeam> seeded = crossSeed(advancingTeams, groups.size(), advPer);
        for (int i = 0; i < koBracket.size() && (i * 2 + 1) < seeded.size(); i++) {
            TournamentMatch m = koBracket.get(i);
            m.setTeamA(seeded.get(i * 2));
            m.setTeamB(seeded.get(i * 2 + 1));
            matchRepo.save(m);
        }
        log.info("Seeded {} advancing teams into knockout bracket", advancingTeams.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ROUND ROBIN — everyone vs everyone
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateRoundRobin(
            TournamentConfig config, List<AuctionTeam> teams) {

        List<TournamentMatch> matches = new ArrayList<>();
        int n = teams.size();
        // Total matches = n*(n-1)/2
        LocalDateTime cursor = config.getStartDate().atTime(9, 0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        int matchNum = 1;

        // ── Berger round-robin algorithm ────────────────────────
        // Fix team[0], rotate team[1..n-1] for each round
        // Produces n-1 rounds with n/2 matches each
        List<AuctionTeam> pool = new ArrayList<>(teams);
        if (n % 2 != 0) pool.add(null); // null = bye
        int totalSlots = pool.size();
        int rounds = totalSlots - 1;

        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < totalSlots / 2; i++) {
                AuctionTeam teamA = pool.get(i);
                AuctionTeam teamB = pool.get(totalSlots - 1 - i);

                if (teamA == null || teamB == null) continue; // bye round

                TournamentMatch m = TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.LEAGUE_MATCH)
                    .roundNumber(round + 1)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(teamA)
                    .teamB(teamB)
                    .scheduledAt(cursor)
                    .durationMinutes(dur)
                    .venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED)
                    .build();

                matches.add(m);
                cursor = cursor.plusMinutes(dur + brk);
            }

            // Rotate: keep pool[0] fixed, rotate rest
            AuctionTeam last = pool.remove(totalSlots - 1);
            pool.add(1, last);
        }

        log.info("[ROUND_ROBIN] {} teams, {} rounds, {} matches",
            n, rounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. DOUBLE ELIMINATION — winners + losers bracket
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateDoubleElimination(
            TournamentConfig config, List<AuctionTeam> teams) {

        List<TournamentMatch> matches = new ArrayList<>();
        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));
        int slotCount = (int) Math.pow(2, rounds);
        List<AuctionTeam> slots = new ArrayList<>(teams);
        while (slots.size() < slotCount) slots.add(null);

        LocalDateTime cursor = config.getStartDate().atTime(9, 0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        int matchNum = 1;

        // ── Winners Bracket ─────────────────────────────────────
        for (int round = 1; round <= rounds; round++) {
            int roundSize = slotCount / (int) Math.pow(2, round);
            for (int i = 0; i < roundSize; i++) {
                TournamentMatch m = TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.WINNERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(round == 1 ? slots.get(i * 2) : null)
                    .teamB(round == 1 ? slots.get(i * 2 + 1) : null)
                    .scheduledAt(cursor)
                    .durationMinutes(dur)
                    .venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED)
                    .build();
                matches.add(m);
                cursor = cursor.plusMinutes(dur + brk);
            }
        }

        // ── Losers Bracket ──────────────────────────────────────
        // LB has 2*(rounds-1) rounds
        int lbRounds = 2 * (rounds - 1);
        for (int round = 1; round <= lbRounds; round++) {
            int lbSize = (int)(slotCount / Math.pow(2, Math.ceil(round / 2.0) + 1));
            lbSize = Math.max(1, lbSize);
            for (int i = 0; i < lbSize; i++) {
                TournamentMatch m = TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.LOSERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(null).teamB(null)  // filled by advance logic
                    .scheduledAt(cursor)
                    .durationMinutes(dur)
                    .venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED)
                    .build();
                matches.add(m);
                cursor = cursor.plusMinutes(dur + brk);
            }
        }

        // ── Grand Final ─────────────────────────────────────────
        // WB winner vs LB winner (with possible reset if WB winner loses)
        TournamentMatch grandFinal = TournamentMatch.builder()
            .config(config).round(MatchRound.GRAND_FINAL).roundNumber(rounds + lbRounds + 1)
            .matchNumber(matchNum++)
            .scheduledAt(cursor).durationMinutes(dur).venueName(config.getVenueName())
            .status(MatchStatus.SCHEDULED).build();
        matches.add(grandFinal);

        log.info("[DOUBLE_ELIM] {} rounds WB, {} rounds LB, {} total matches",
            rounds, lbRounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. SWISS SYSTEM — score-paired rounds
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateSwissRound1(
            TournamentConfig config, List<AuctionTeam> teams) {

        int rounds = Objects.requireNonNullElse(
            config.getSwissRounds(),
            (int) Math.ceil(Math.log(teams.size()) / Math.log(2)));

        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = config.getStartDate().atTime(9, 0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Round 1: random pairing
        List<AuctionTeam> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);
        List<TournamentMatch> round1 = pairSwissRound(config, shuffled, 1, cursor, dur, brk);
        matches.addAll(round1);

        log.info("[SWISS] {} rounds planned, round 1 generated ({} matches)",
            rounds, round1.size());
        return matches;
    }

    /** Called after each Swiss round result to generate next round pairing */
    @Transactional
    public List<TournamentMatch> generateNextSwissRound(Long configId) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow();

        int lastRound = matchRepo.findMaxSwissRound(configId).orElse(0);
        int nextRound = lastRound + 1;
        int maxRounds = Objects.requireNonNullElse(config.getSwissRounds(), 5);

        if (nextRound > maxRounds)
            throw new IllegalStateException("Swiss tournament already completed " + maxRounds + " rounds");

        // Build score map: teamId → points
        Map<Long, Integer> scores = new HashMap<>();
        matchRepo.findByConfigIdOrderBySwissRoundNumber(configId).stream()
            .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
            .forEach(m -> {
                Long wId = m.getWinner().getId();
                Long lId = wId.equals(m.getTeamA().getId())
                    ? m.getTeamB().getId() : m.getTeamA().getId();
                scores.merge(wId, config.getPointsForWin() != null ? config.getPointsForWin() : 2, Integer::sum);
                scores.merge(lId, config.getPointsForLoss() != null ? config.getPointsForLoss() : 0, Integer::sum);
            });

        // Get all teams, sort by score descending (same score → random)

        // In real app, we need to query based on eventId, since config links to community, we should probably fetch teams related to community. But we use eventId in AuctionTeam. 
        // For now, let's fetch all teams and filter in memory, or use teamRepo.findAllById(). Since tournament uses specific teams, we should extract teams from previous matches
        Set<Long> teamIdsInConfig = new HashSet<>();
        matchRepo.findByConfigId(configId).forEach(m -> {
            if (m.getTeamA() != null) teamIdsInConfig.add(m.getTeamA().getId());
            if (m.getTeamB() != null) teamIdsInConfig.add(m.getTeamB().getId());
        });
        List<AuctionTeam> tournamentTeams = teamRepo.findAllById(teamIdsInConfig);

        tournamentTeams.sort((a, b) -> {
            int sa = scores.getOrDefault(a.getId(), 0);
            int sb = scores.getOrDefault(b.getId(), 0);
            if (sa != sb) return sb - sa;
            return (int)(Math.random() * 3 - 1); // random tiebreak
        });

        // Build set of already-played pairs to avoid rematches
        Set<String> playedPairs = matchRepo.findByConfigId(configId).stream()
            .filter(m -> m.getTeamA() != null && m.getTeamB() != null)
            .map(m -> Math.min(m.getTeamA().getId(), m.getTeamB().getId()) + "-"
                    + Math.max(m.getTeamA().getId(), m.getTeamB().getId()))
            .collect(Collectors.toSet());

        // Pair adjacent teams by score, avoid rematches
        List<AuctionTeam> paired = swissPairNoRematches(tournamentTeams, playedPairs);
        LocalDateTime cursor = LocalDateTime.now()
            .plusDays(1).withHour(9).withMinute(0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        List<TournamentMatch> newMatches = pairSwissRound(config, paired, nextRound, cursor, dur, brk);
        matchRepo.saveAll(newMatches);
        log.info("[SWISS] Round {} generated ({} matches)", nextRound, newMatches.size());
        return newMatches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. SUPER LEAGUE — IPL-style top-4 playoffs
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateSuperLeague(
            TournamentConfig config, List<AuctionTeam> teams) {

        // Phase 1: Full round-robin league stage (everyone vs everyone)
        List<TournamentMatch> leagueMatches = generateRoundRobin(config, teams);

        // Phase 2: Playoffs for top 4
        // Q1: #1 vs #2 → winner goes to Final, loser to Q2
        // Eliminator: #3 vs #4 → loser eliminated, winner to Q2
        // Q2: Q1-loser vs Eliminator-winner → winner goes to Final
        // Final: Q1-winner vs Q2-winner
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        LocalDateTime playoffStart = config.getStartDate()
            .plusDays((long) leagueMatches.size() / 2 + 1).atTime(19, 0);

        List<TournamentMatch> playoffs = List.of(
            buildPlayoffMatch(config, MatchRound.QUALIFIER_1,  1, playoffStart,              dur),
            buildPlayoffMatch(config, MatchRound.ELIMINATOR,   2, playoffStart.plusDays(1),   dur),
            buildPlayoffMatch(config, MatchRound.QUALIFIER_2,  3, playoffStart.plusDays(3),   dur),
            buildPlayoffMatch(config, MatchRound.SUPER_FINAL,  4, playoffStart.plusDays(5),   dur)
        );

        List<TournamentMatch> all = new ArrayList<>(leagueMatches);
        all.addAll(playoffs);
        log.info("[SUPER_LEAGUE] {} league + {} playoff = {} total",
            leagueMatches.size(), playoffs.size(), all.size());
        return all;
    }

    // ═══════════════════════════════════════════════════════════════
    // STANDINGS UPDATE
    // ═══════════════════════════════════════════════════════════════
    private void updateGroupStandings(TournamentMatch match,
            AuctionTeam winner, AuctionTeam loser, MatchResultRequest req) {

        int ptsWin  = Objects.requireNonNullElse(match.getConfig().getPointsForWin(), 2);
        int ptsLoss = Objects.requireNonNullElse(match.getConfig().getPointsForLoss(), 0);

        // Update winner
        GroupTeamStanding ws = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), winner.getId());
        ws.setPlayed(ws.getPlayed() + 1);
        ws.setWon(ws.getWon() + 1);
        ws.setPoints(ws.getPoints() + ptsWin);
        if (req.runsTeamA() != null) updateNRR(ws, winner, match, req);
        standingRepo.save(ws);

        // Update loser
        GroupTeamStanding ls = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), loser.getId());
        ls.setPlayed(ls.getPlayed() + 1);
        ls.setLost(ls.getLost() + 1);
        ls.setPoints(ls.getPoints() + ptsLoss);
        if (req.runsTeamB() != null) updateNRR(ls, loser, match, req);
        standingRepo.save(ls);
    }

    private void updateNRR(GroupTeamStanding standing, AuctionTeam team,
            TournamentMatch match, MatchResultRequest req) {
        boolean isTeamA = team.getId().equals(match.getTeamA().getId());
        int runsFor     = isTeamA ? req.runsTeamA() : req.runsTeamB();
        int runsAgainst = isTeamA ? req.runsTeamB() : req.runsTeamA();
        int oversFor    = isTeamA ? req.oversTeamA() : req.oversTeamB();
        int oversAgainst= isTeamA ? req.oversTeamB() : req.oversTeamA();

        standing.setRunsFor(Objects.requireNonNullElse(standing.getRunsFor(), 0) + runsFor);
        standing.setRunsAgainst(Objects.requireNonNullElse(standing.getRunsAgainst(), 0) + runsAgainst);
        standing.setOversFor(Objects.requireNonNullElse(standing.getOversFor(), 0) + oversFor);
        standing.setOversAgainst(Objects.requireNonNullElse(standing.getOversAgainst(), 0) + oversAgainst);

        double nrr = (standing.getOversFor() > 0 ? (double) standing.getRunsFor() / standing.getOversFor() : 0)
                   - (standing.getOversAgainst() > 0 ? (double) standing.getRunsAgainst() / standing.getOversAgainst() : 0);
        standing.setNetRunRate(Math.round(nrr * 1000.0) / 1000.0);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════
    private MatchRound roundLabel(int totalRounds, int round) {
        int fromEnd = totalRounds - round;
        return switch (fromEnd) {
            case 0 -> MatchRound.FINAL;
            case 1 -> MatchRound.SEMI_FINAL;
            case 2 -> MatchRound.QUARTER_FINAL;
            case 3 -> MatchRound.ROUND_OF_16;
            case 4 -> MatchRound.ROUND_OF_32;
            default -> MatchRound.ROUND_OF_64;
        };
    }

    private List<AuctionTeam> seedTeams(List<AuctionTeam> teams) {
        // Seed by ranking (could be based on past performance in DB)
        // For now, preserve input order as seeding
        return teams;
    }

    private List<AuctionTeam> crossSeed(
            List<AuctionTeam> advancing, int nGroups, int advPer) {
        // Standard cross-seeding: A1 vs B2, B1 vs A2
        List<AuctionTeam> result = new ArrayList<>();
        for (int i = 0; i < advPer; i++) {
            for (int g = 0; g < nGroups; g++) {
                int idx = g * advPer + i;
                if (idx < advancing.size()) result.add(advancing.get(idx));
            }
        }
        // Reorder for bracket: A1 vs B2, A2 vs B1
        if (result.size() >= 4) {
            Collections.swap(result, 1, 2); // A1 vs B2, B1 vs A2
        }
        return result;
    }

    private List<TournamentMatch> roundRobinMatchesForGroup(
            TournamentGroup grp, List<AuctionTeam> teams,
            TournamentConfig config, LocalDateTime start, int dur, int brk) {

        List<TournamentMatch> matches = new ArrayList<>();
        int matchNum = 1;
        LocalDateTime cursor = start;

        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                matches.add(TournamentMatch.builder()
                    .config(config).group(grp)
                    .round(MatchRound.GROUP_STAGE).roundNumber(matchNum).matchNumber(matchNum)
                    .teamA(teams.get(i)).teamB(teams.get(j))
                    .scheduledAt(cursor).durationMinutes(dur).venueName(config.getVenueName())
                    .status(MatchStatus.SCHEDULED).build());
                matchNum++;
                cursor = cursor.plusMinutes(dur + brk);
            }
        }
        return matches;
    }

    private List<TournamentMatch> pairSwissRound(
            TournamentConfig config, List<AuctionTeam> teams,
            int roundNum, LocalDateTime start, int dur, int brk) {

        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = start;
        for (int i = 0; i + 1 < teams.size(); i += 2) {
            matches.add(TournamentMatch.builder()
                .config(config).round(MatchRound.SWISS_ROUND)
                .roundNumber(roundNum).swissRoundNumber(roundNum)
                .matchNumber(i / 2 + 1).bracketSlot(i / 2)
                .teamA(teams.get(i)).teamB(teams.get(i + 1))
                .scheduledAt(cursor).durationMinutes(dur).venueName(config.getVenueName())
                .status(MatchStatus.SCHEDULED).build());
            cursor = cursor.plusMinutes(dur + brk);
        }
        return matches;
    }

    private List<AuctionTeam> swissPairNoRematches(
            List<AuctionTeam> sorted, Set<String> playedPairs) {

        List<AuctionTeam> result = new ArrayList<>();
        boolean[] used = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) continue;
            for (int j = i + 1; j < sorted.size(); j++) {
                if (used[j]) continue;
                String key = Math.min(sorted.get(i).getId(), sorted.get(j).getId()) + "-"
                           + Math.max(sorted.get(i).getId(), sorted.get(j).getId());
                if (!playedPairs.contains(key)) {
                    result.add(sorted.get(i));
                    result.add(sorted.get(j));
                    used[i] = used[j] = true;
                    break;
                }
            }
        }
        return result;
    }

    private TournamentMatch buildPlayoffMatch(
            TournamentConfig config, MatchRound round,
            int roundNum, LocalDateTime at, int dur) {
        return TournamentMatch.builder()
            .config(config).round(round).roundNumber(roundNum).matchNumber(1)
            .scheduledAt(at).durationMinutes(dur).venueName(config.getVenueName())
            .status(MatchStatus.SCHEDULED).build();
    }

    private void wireKnockoutLinks(List<TournamentMatch> matches) {
        // Group by roundNumber
        Map<Integer, List<TournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(TournamentMatch::getRoundNumber));

        byRound.forEach((round, rMatches) -> {
            List<TournamentMatch> nextRound = byRound.get(round + 1);
            if (nextRound == null) return;
            for (int i = 0; i < rMatches.size(); i++) {
                TournamentMatch current = rMatches.get(i);
                TournamentMatch next    = nextRound.get(i / 2);
                current.setWinnerFeedFromMatchA(
                    (i % 2 == 0) ? current.getId() : current.getWinnerFeedFromMatchA());
                // Links wired after DB save so IDs are available
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // SAVE / UPDATE config (without generating schedule)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public TournamentConfig saveTournamentConfig(TournamentConfigRequest req, Long adminUserId) {
        TournamentConfig config = buildConfig(req, adminUserId);
        config = configRepo.save(config);
        log.info("Tournament config saved (DRAFT): [{}] type={}", config.getTournamentName(), config.getTournamentType());
        return config;
    }

    @Transactional
    public TournamentConfig updateTournamentConfig(Long configId, TournamentConfigRequest req) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow(
            () -> new IllegalArgumentException("Tournament config not found: " + configId));

        config.setTournamentName(req.tournamentName());
        config.setSport(sportMetaRepo.getReferenceById(req.sportId()));
        config.setCommunity(communityRepo.getReferenceById(req.communityId()));
        if (req.eventId() != null) config.setEvent(eventRepo.getReferenceById(req.eventId()));
        config.setTournamentType(TournamentType.valueOf(req.tournamentType()));
        config.setTotalTeams(req.totalTeams());
        config.setNumberOfGroups(req.numberOfGroups());
        config.setTeamsAdvancingPerGroup(req.teamsAdvancingPerGroup());
        config.setSwissRounds(req.swissRounds());
        config.setThirdPlaceMatch(Objects.requireNonNullElse(req.thirdPlaceMatch(), config.getThirdPlaceMatch()));
        config.setHasSeeding(Objects.requireNonNullElse(req.hasSeeding(), config.getHasSeeding()));
        config.setStartDate(req.startDate());
        config.setEndDate(req.endDate());
        config.setMatchDurationMinutes(Objects.requireNonNullElse(req.matchDurationMinutes(), 90));
        config.setBreakBetweenMatchesMinutes(Objects.requireNonNullElse(req.breakBetweenMatchesMinutes(), 30));
        config.setVenueName(req.venueName());
        config.setPointsForWin(Objects.requireNonNullElse(req.pointsForWin(), 2));
        config.setPointsForDraw(Objects.requireNonNullElse(req.pointsForDraw(), 1));
        config.setPointsForLoss(Objects.requireNonNullElse(req.pointsForLoss(), 0));
        config = configRepo.save(config);
        log.info("Tournament config updated: [{}]", config.getTournamentName());
        return config;
    }

    public TournamentConfigResponse toConfigResponse(TournamentConfig c) {
        int matchCount = matchRepo.findByConfigId(c.getId()).size();
        return new TournamentConfigResponse(
            c.getId(), c.getTournamentName(), c.getTournamentType().name(),
            c.getSport() != null ? c.getSport().getId() : null,
            c.getSport() != null ? c.getSport().getName() : null,
            c.getCommunity() != null ? c.getCommunity().getId() : null,
            c.getCommunity() != null ? c.getCommunity().getName() : null,
            c.getEvent() != null ? c.getEvent().getId() : null,
            c.getEvent() != null ? c.getEvent().getName() : null,
            c.getTotalTeams(), c.getNumberOfGroups(), c.getTeamsPerGroup(),
            c.getTeamsAdvancingPerGroup(), c.getThirdPlaceMatch(), c.getHasSeeding(),
            c.getSwissRounds(), c.getStartDate(), c.getEndDate(),
            c.getMatchDurationMinutes(), c.getBreakBetweenMatchesMinutes(),
            c.getVenueName(), c.getPointsForWin(), c.getPointsForDraw(),
            c.getPointsForLoss(), c.getStatus().name(), matchCount,
            c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    public List<TournamentConfigResponse> getConfigsByCommunity(Long communityId) {
        return configRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
            .stream().map(this::toConfigResponse).toList();
    }

    private TournamentConfig buildConfig(TournamentConfigRequest req, Long adminUserId) {
        TournamentConfig.TournamentConfigBuilder builder = TournamentConfig.builder()
            .tournamentName(req.tournamentName())
            .sport(sportMetaRepo.getReferenceById(req.sportId()))
            .community(communityRepo.getReferenceById(req.communityId()))
            .tournamentType(TournamentType.valueOf(req.tournamentType()))
            .totalTeams(req.totalTeams())
            .numberOfGroups(req.numberOfGroups())
            .teamsAdvancingPerGroup(req.teamsAdvancingPerGroup())
            .swissRounds(req.swissRounds())
            .thirdPlaceMatch(Objects.requireNonNullElse(req.thirdPlaceMatch(), true))
            .hasSeeding(Objects.requireNonNullElse(req.hasSeeding(), false))
            .startDate(req.startDate())
            .endDate(req.endDate())
            .matchDurationMinutes(Objects.requireNonNullElse(req.matchDurationMinutes(), 90))
            .breakBetweenMatchesMinutes(Objects.requireNonNullElse(req.breakBetweenMatchesMinutes(), 30))
            .venueName(req.venueName())
            .pointsForWin(Objects.requireNonNullElse(req.pointsForWin(), 2))
            .pointsForDraw(Objects.requireNonNullElse(req.pointsForDraw(), 1))
            .pointsForLoss(Objects.requireNonNullElse(req.pointsForLoss(), 0))
            .status(TournamentConfig.TournamentStatus.DRAFT)
            .createdBy(userRepo.getReferenceById(adminUserId));

        if (req.eventId() != null) {
            builder.event(eventRepo.getReferenceById(req.eventId()));
        }
        return builder.build();
    }

    private TournamentScheduleResponse buildResponse(TournamentConfig config) {
        List<TournamentMatch> matches = matchRepo.findByConfigIdOrderByScheduledAt(config.getId());
        List<TournamentGroup> groups  = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());

        int totalRounds = matches.stream()
            .mapToInt(TournamentMatch::getRoundNumber).max().orElse(0);

        // Build group responses
        List<GroupResponse> groupResponses = groups.stream().map(g -> {
            List<StandingResponse> standings = standingRepo
                .findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())
                .stream().map(this::toStandingResponse)
                .collect(Collectors.toList());
            List<MatchResponse> gMatches = matches.stream()
                .filter(m -> g.equals(m.getGroup()))
                .map(this::toMatchResponse).toList();
            return new GroupResponse(g.getId(), g.getGroupName(), standings, gMatches);
        }).toList();

        // Build round responses
        Map<Integer, List<TournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(TournamentMatch::getRoundNumber));
        List<RoundResponse> rounds = byRound.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new RoundResponse(
                e.getValue().get(0).getRound().name(),
                e.getKey(),
                e.getValue().stream().map(this::toMatchResponse).toList()))
            .toList();

        return new TournamentScheduleResponse(
            config.getId(), config.getTournamentName(), config.getTournamentType().name(),
            config.getTotalTeams(), matches.size(), totalRounds,
            config.getStartDate(), config.getEndDate(),
            groupResponses, rounds,
            matches.stream().map(this::toMatchResponse).toList()
        );
    }

    public MatchResponse toMatchResponse(TournamentMatch m) {
        return new MatchResponse(
            m.getId(), m.getRound() != null ? m.getRound().name() : "TBD",
            Objects.requireNonNullElse(m.getMatchNumber(), 0),
            Objects.requireNonNullElse(m.getBracketSlot(), 0),
            m.getTeamA() != null ? m.getTeamA().getTeamName() : "TBD",
            m.getTeamB() != null ? m.getTeamB().getTeamName() : "TBD",
            m.getTeamA() != null ? m.getTeamA().getColorHex() : "#555",
            m.getTeamB() != null ? m.getTeamB().getColorHex() : "#555",
            m.getScheduledAt() != null ? m.getScheduledAt().toString() : "",
            m.getVenueName(), m.getStatus().name(),
            m.getScoreTeamA(), m.getScoreTeamB(),
            m.getWinner() != null ? m.getWinner().getTeamName() : null,
            m.getWinnerAdvancesToMatchId(),
            m.getStatus() == MatchStatus.BYE
        );
    }

    private StandingResponse toStandingResponse(GroupTeamStanding s) {
        return new StandingResponse(
            0, s.getTeam().getId(), s.getTeam().getTeamName(),
            s.getTeam().getColorHex(),
            s.getPlayed(), s.getWon(), s.getLost(), s.getDrawn(), s.getPoints(),
            Objects.requireNonNullElse(s.getNetRunRate(), 0.0), s.getQualified()
        );
    }
    // ═══════════════════════════════════════════════════════════════
    // MANUAL SCHEDULING (Drag & Drop UI Integration)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public void assignTeamsToGroups(Long configId, List<GroupAssignmentRequest> assignments) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow();
        
        // Clear existing group standings for manual mode override
        List<TournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(configId);
        groups.forEach(g -> standingRepo.deleteAll(standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())));

        // Group the assignments by groupId
        Map<String, List<GroupAssignmentRequest>> byGroup = assignments.stream()
            .collect(Collectors.groupingBy(GroupAssignmentRequest::groupId));

        int groupOrder = 1;
        for (Map.Entry<String, List<GroupAssignmentRequest>> entry : byGroup.entrySet()) {
            String groupName = entry.getKey();
            if ("UNASSIGNED".equals(groupName)) continue; // don't create a group for unassigned

            // Check if group exists, else create
            TournamentGroup grp = groups.stream()
                .filter(g -> g.getGroupName().equals(groupName))
                .findFirst()
                .orElseGet(() -> {
                    TournamentGroup newGrp = TournamentGroup.builder()
                        .config(config)
                        .groupName(groupName)
                        .groupOrder(0) // update later
                        .build();
                    return groupRepo.save(newGrp);
                });
            grp.setGroupOrder(groupOrder++);
            groupRepo.save(grp);

            for (GroupAssignmentRequest req : entry.getValue()) {
                AuctionTeam team = teamRepo.findById(req.teamId()).orElseThrow();
                standingRepo.save(GroupTeamStanding.builder()
                    .group(grp)
                    .team(team)
                    .seedRank(0)
                    .build());
            }
        }
    }

    @Transactional
    public void scheduleManualMatch(Long configId, MatchScheduleRequest req) {
        TournamentConfig config = configRepo.findById(configId).orElseThrow();
        
        AuctionTeam home = req.homeTeamId() != null ? teamRepo.findById(req.homeTeamId()).orElse(null) : null;
        AuctionTeam away = req.awayTeamId() != null ? teamRepo.findById(req.awayTeamId()).orElse(null) : null;
        
        // Find group if it's a group stage match
        TournamentGroup group = null;
        if ("GROUP_STAGE".equals(req.matchType())) {
            group = groupRepo.findByConfigIdOrderByGroupOrder(configId).stream()
                .filter(g -> g.getGroupName().replace(" ", "_").toUpperCase().equals(req.stage().replace(" ", "_").toUpperCase()))
                .findFirst()
                .orElse(null);
        }

        TournamentMatch m = TournamentMatch.builder()
            .config(config)
            .group(group)
            .round("GROUP_STAGE".equals(req.matchType()) ? MatchRound.GROUP_STAGE : MatchRound.valueOf(req.stage()))
            .teamA(home)
            .teamB(away)
            .scheduledAt(LocalDateTime.parse(req.startTime()))
            .durationMinutes(config.getMatchDurationMinutes() != null ? config.getMatchDurationMinutes() : 90)
            .venueName(config.getVenueName())
            .status(MatchStatus.SCHEDULED)
            .build();
            
        matchRepo.save(m);
    }
}
