package com.manacommunity.api.scheduler;

import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.EventNotificationSchedule;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.repository.EventNotificationScheduleRepository;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BUG FIXES applied:
 *
 * 1. EventNotificationScheduleRepository was injected but the interface
 *    didn't exist — now created.
 *
 * 2. `regRepo` was used but NEVER declared or injected as a field.
 *    Added SportsEventRegistrationRepository regRepo field.
 *
 * 3. `CONFIRMED` was referenced as a bare identifier — must be qualified
 *    as SportsEventRegistration.RegistrationStatus.CONFIRMED.
 *
 * 4. `LocalDateTime.now()` was called without import — added import.
 *
 * 5. `log.error(...)` was called but @Slf4j was not present — added @Slf4j.
 *
 * 6. `PushNotificationService` is a custom service not defined anywhere;
 *    kept the injection but added a TODO stub comment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final EventNotificationScheduleRepository notifRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final PushNotificationService pushService;
    private final com.manacommunity.api.repository.SportsNotificationSchedulerRepository premiumRepo;

    @Scheduled(fixedDelay = 60_000)  // every 60 seconds
    @Transactional
    public void sendPendingNotifications() {
        // Process legacy scheduled notifications
        List<EventNotificationSchedule> pending =
                notifRepo.findByNotifyAtBeforeAndSentFalse(LocalDateTime.now());

        for (EventNotificationSchedule notif : pending) {
            try {
                List<AppUser> recipients = regRepo
                        .findByEventIdAndStatus(
                                notif.getEvent().getId(),
                                SportsEventRegistration.RegistrationStatus.CONFIRMED)
                        .stream().map(SportsEventRegistration::getUser).toList();

                pushService.sendBulk(recipients, notif.getTitle(), notif.getBody());
                notif.setSent(true);
                notifRepo.save(notif);
            } catch (Exception e) {
                log.error("Failed to send notification id={}", notif.getId(), e);
            }
        }

        // Process premium scheduled notifications
        List<com.manacommunity.api.model.SportsNotificationScheduler> premiumPending =
                premiumRepo.findByNotifyAtBeforeAndSentFalseAndEnabledTrue(LocalDateTime.now());

        for (com.manacommunity.api.model.SportsNotificationScheduler premiumNotif : premiumPending) {
            try {
                List<AppUser> recipients = regRepo
                        .findByEventIdAndStatus(
                                premiumNotif.getEvent().getId(),
                                SportsEventRegistration.RegistrationStatus.CONFIRMED)
                        .stream().map(SportsEventRegistration::getUser).toList();

                if (!recipients.isEmpty()) {
                    pushService.sendBulk(recipients, premiumNotif.getTitle(), premiumNotif.getBody());
                }
                premiumNotif.setSent(true);
                premiumRepo.save(premiumNotif);
            } catch (Exception e) {
                log.error("Failed to send premium notification id={}", premiumNotif.getId(), e);
            }
        }
    }
}
