package com.manacommunity.api.dto.scheduler;

import jakarta.validation.constraints.NotNull;

public record MatchResultRequest(
    @NotNull Long    matchId,
    @NotNull Long    winnerTeamId,
    String           scoreTeamA,
    String           scoreTeamB,
    String           matchNotes,
    // For NRR calculation
    Integer          runsTeamA,
    Integer          runsTeamB,
    Integer          oversTeamA,
    Integer          oversTeamB
) {}
