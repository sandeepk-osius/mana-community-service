package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BUG FIX: Repository was missing entirely. SportsEventServiceImpl and
 * SportsController both inject SportMetaRepository but no such interface
 * existed, causing a Spring context startup failure.
 */
@Repository
public interface SportMetaRepository extends JpaRepository<SportsMeta, Long> {
    /** Used by SportsController.getAllSports() for dropdown population. */
    List<SportsMeta> findByActiveTrue();
    
    java.util.Optional<SportsMeta> findByNameIgnoreCase(String name);
}
