package com.manacommunity.api.model.scheduler;

public enum MatchStatus {
    DRAFT,        // saved as draft — visible only to the organiser
    PUBLISHED,    // published — visible to all participants
    SCHEDULED,
    LIVE,
    COMPLETED,
    POSTPONED,
    CANCELLED,
    BYE           // auto-advance when team count is not a power of 2
}
