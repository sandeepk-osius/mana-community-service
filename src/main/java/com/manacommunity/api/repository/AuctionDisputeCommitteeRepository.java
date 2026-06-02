package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionDisputeCommittee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionDisputeCommitteeRepository extends JpaRepository<AuctionDisputeCommittee, Long> {
}
