package com.manacommunity.api.model.scheduler;

public enum MatchStatus {
    SCHEDULED,
    LIVE,
    COMPLETED,
    POSTPONED,
    CANCELLED,
    BYE           // auto-advance when team count is not a power of 2
}
