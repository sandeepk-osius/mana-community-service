package com.manacommunity.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_sponsor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnoreProperties({"sponsors"})
    private SportsEvent event;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
