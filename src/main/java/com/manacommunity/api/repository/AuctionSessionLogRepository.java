package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionSessionLogRepository extends JpaRepository<AuctionSessionLog, Long> {
}
