package com.manacommunity.api.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.PlayerCategory;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.SportsEventRegistration;
import com.manacommunity.api.repository.PlayerCategoryRepository;
import com.manacommunity.api.repository.SportsEventRegistrationRepository;
import com.manacommunity.api.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportsEventCsvImportService {

    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final PlayerCategoryRepository categoryRepo;

    public record ImportResult(int imported, int skipped, List<String> skippedReasons) {}

    @Transactional
    public ImportResult importRegistrations(Long eventId, MultipartFile file) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        int imported = 0;
        List<String> skippedReasons = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) { isHeader = false; continue; }
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 1 || cols[0].isBlank()) {
                    skippedReasons.add("Row " + lineNum + ": empty player name");
                    continue;
                }

                String playerName = cols[0].trim();
                String categoryName = cols.length > 1 ? cols[1].trim() : "";
                String ageStr = cols.length > 2 ? cols[2].trim() : "";
                String flatNumber = cols.length > 3 ? cols[3].trim() : null;
                String relation = cols.length > 4 ? cols[4].trim() : null;
                String role = cols.length > 5 ? cols[5].trim() : null;

                // Duplicate check
                if (regRepo.existsByEventIdAndUserIsNullAndPlayerName(eventId, playerName)) {
                    skippedReasons.add("Row " + lineNum + ": '" + playerName + "' already registered");
                    continue;
                }

                // Max participants guard
                if (regRepo.countByEventId(eventId) >= event.getMaxParticipants()) {
                    skippedReasons.add("Row " + lineNum + ": event is full (max " + event.getMaxParticipants() + ")");
                    break;
                }

                // Resolve category
                PlayerCategory category = null;
                if (!categoryName.isEmpty()) {
                    category = categoryRepo.findByNameIgnoreCase(categoryName).orElse(null);
                    if (category == null) {
                        skippedReasons.add("Row " + lineNum + ": unknown category '" + categoryName + "'");
                        continue;
                    }
                }

                // Parse age
                Integer age = null;
                if (!ageStr.isEmpty()) {
                    try { age = Integer.parseInt(ageStr); } catch (NumberFormatException ignored) {}
                }

                // Derive matchType from category name
                SportsEvent.MatchFormat matchType = deriveMatchType(categoryName);

                SportsEventRegistration reg = SportsEventRegistration.builder()
                        .event(event)
                        .user(null)
                        .category(category)
                        .matchType(matchType)
                        .playerName(playerName)
                        .flatNumber(flatNumber)
                        .relation(relation)
                        .age(age)
                        .role(role)
                        .status(SportsEventRegistration.RegistrationStatus.REGISTERED)
                        .registeredAt(LocalDateTime.now())
                        .build();

                regRepo.save(reg);
                imported++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage(), e);
        }

        return new ImportResult(imported, skippedReasons.size(), skippedReasons);
    }

    private SportsEvent.MatchFormat deriveMatchType(String categoryName) {
        if (categoryName == null) return SportsEvent.MatchFormat.SINGLES;
        String lower = categoryName.toLowerCase();
        if (lower.contains("mixed")) return SportsEvent.MatchFormat.MIXED_DOUBLES;
        if (lower.contains("double")) return SportsEvent.MatchFormat.DOUBLES;
        if (lower.contains("team")) return SportsEvent.MatchFormat.TEAM;
        return SportsEvent.MatchFormat.SINGLES;
    }
}
