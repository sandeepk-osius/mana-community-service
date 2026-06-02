package com.manacommunity.api.repository;

import com.manacommunity.api.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * BUG FIX: Added missing query methods used by SportsEventServiceImpl:
 * - countByEventId()  — participant cap enforcement
 * - findByEventIdAndStatus() — used by NotificationScheduler to find recipients
 */
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserIdAndPlayerName(Long eventId, Long userId, String playerName);
    long countByEventId(Long eventId);
    List<EventRegistration> findByEventId(Long eventId);
    List<EventRegistration> findByUserId(Long userId);
    List<EventRegistration> findByEventIdAndStatus(Long eventId, EventRegistration.RegistrationStatus status);
}
