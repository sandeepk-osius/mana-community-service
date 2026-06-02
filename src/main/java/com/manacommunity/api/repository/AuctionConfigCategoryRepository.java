package com.manacommunity.api.repository;

import com.manacommunity.api.model.AuctionConfigCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionConfigCategoryRepository extends JpaRepository<AuctionConfigCategory, Long> {
}
