package com.manacommunity.api.repository;

import com.manacommunity.api.model.SportsNotificationScheduler;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SportsNotificationSchedulerRepository extends JpaRepository<SportsNotificationScheduler, Long> {
    List<SportsNotificationScheduler> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
    List<SportsNotificationScheduler> findByNotifyAtBeforeAndSentFalseAndEnabledTrue(java.time.LocalDateTime now);
}
