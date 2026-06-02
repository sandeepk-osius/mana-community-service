package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * BUG FIX: EventNotificationSchedule was an empty stub class.
 * Now fully mapped to event_notification_schedule table.
 * - NotificationScheduler reads notif.getEvent().getId(), notif.getTitle(),
 *   notif.getBody(), notif.setSent() — all fields must exist.
 * - SportsEventServiceImpl.scheduleNotifications() calls
 *   EventNotificationSchedule.builder() — requires @Builder.
 */
@Entity
@Table(name = "event_notification_schedule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventNotificationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private SportsEvent event;

    @Column(name = "notify_at", nullable = false)
    private LocalDateTime notifyAt;

    @Column(nullable = false, length = 30)
    private String type; // REGISTRATION_REMINDER, MATCH_REMINDER, RESULT

    @Column(length = 100)
    private String title;

    @Column(length = 300)
    private String body;

    @Column(nullable = false)
    private Boolean sent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
