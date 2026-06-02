package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;

public record SoldPlayerRequest(
    @NotNull Long playerId,
    @NotNull Long teamId
) {}
