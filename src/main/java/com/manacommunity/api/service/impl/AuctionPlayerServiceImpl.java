package com.manacommunity.api.service.impl;

import com.manacommunity.api.model.AuctionPlayer;
import com.manacommunity.api.repository.AuctionPlayerRepository;
import com.manacommunity.api.service.AuctionPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionPlayerServiceImpl implements AuctionPlayerService {

    private final AuctionPlayerRepository auctionPlayerRepository;

    @Override
    public AuctionPlayer savePlayer(AuctionPlayer player) {
        return auctionPlayerRepository.save(player);
    }
}