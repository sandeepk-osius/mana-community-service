package com.manacommunity.api.dto.scheduler;

public record MatchResponse(
    Long    matchId,
    String  roundName,
    int     matchNumber,
    int     bracketSlot,
    String  teamAName,
    String  teamBName,
    String  teamAColor,
    String  teamBColor,
    String  scheduledAt,
    String  venue,
    String  status,
    String  scoreTeamA,
    String  scoreTeamB,
    String  winnerName,
    Long    winnerAdvancesToMatchId,
    boolean isBye
) {}
