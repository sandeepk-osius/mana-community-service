package com.manacommunity.api.dto.scheduler;

/**
 * Stateless input for generating a playoff ("rounds to final") bracket.
 * Mirrors the UI's PlayoffScheduleInput (playoffSchedule.ts) so the bracket
 * the server produces is identical to what the browser used to compute.
 */
public record PlayoffGenerateRequest(
    int     numGroups,
    int     proceedersPerGroup,
    String  seedingOrder,           // "TRADITIONAL" | "SEQUENTIAL" | "RANDOM"
    boolean thirdPlaceMatch,
    String  startDate,              // "yyyy-MM-dd"
    String  startTime,              // 12-hour "hh:mm AM/PM"
    int     matchDurationMinutes,
    int     breakMinutes,
    Long    venueId,
    Long    courtId
) {}
