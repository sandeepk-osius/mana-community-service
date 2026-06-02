package com.manacommunity.api.model.scheduler;

public enum TournamentType {
    /** Single elimination — lose once and you're out */
    KNOCKOUT,

    /** Groups (A, B …) → top N per group advance to knockout */
    GROUP_KNOCKOUT,

    /** Every team plays every other team once (league table) */
    ROUND_ROBIN,

    /** Winners bracket + Losers bracket — need two losses to exit */
    DOUBLE_ELIMINATION,

    /** Paired by score each round — no repeat match-ups */
    SWISS,

    /** Top teams play more games; designed like IPL */
    SUPER_LEAGUE
}
