package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * BUG FIX: Repository was missing entirely. AuctionServiceImpl injects
 * AuctionEventRepository but no such interface existed, causing a Spring
 * context startup failure.
 */
@Repository
public interface AuctionEventRepository extends JpaRepository<AuctionEvent, Long> {
}
