package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity @Table(name = "event_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PlayerCategory category;

    @Enumerated(EnumType.STRING)
    private SportsEvent.MatchFormat matchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_user_id")
    private AppUser partner;

    @Enumerated(EnumType.STRING)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    private String playerName;
    private String relation;
    private String flatNumber;
    private Integer age;
    private String role;

    private LocalDateTime registeredAt;

    public enum RegistrationStatus { PENDING, CONFIRMED, WITHDRAWN }
}
