package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionConfigRepository extends JpaRepository<AuctionConfig, Long> {
    List<AuctionConfig> findBySportIdOrderByCreatedAtDesc(Long sportId);
    List<AuctionConfig> findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(Long sportId, Long communityId);
    List<AuctionConfig> findByCreatedByCommunityIdOrderByCreatedAtDesc(Long communityId);
    Optional<AuctionConfig> findBySportIdAndStatus(Long sportId, AuctionConfig.AuctionStatus status);
    Optional<AuctionConfig> findByEventId(Long eventId);
    boolean existsBySportIdAndSeasonName(Long sportId, String seasonName);
}
