package com.manacommunity.api.repository;

import com.manacommunity.api.model.EventNotificationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BUG FIX: Repository was missing entirely. NotificationScheduler and
 * SportsEventServiceImpl both inject EventNotificationScheduleRepository
 * but no such interface existed, causing a Spring context startup failure.
 */
@Repository
public interface EventNotificationScheduleRepository extends JpaRepository<EventNotificationSchedule, Long> {

    /** Used by NotificationScheduler to fetch unsent, overdue notifications. */
    List<EventNotificationSchedule> findByNotifyAtBeforeAndSentFalse(LocalDateTime now);

    void deleteByEventId(Long eventId);
}
