package com.manacommunity.api.service;

import com.manacommunity.api.model.AuctionConfig;
import com.manacommunity.api.model.AuctionPlayer;
import com.manacommunity.api.repository.AuctionConfigRepository;
import com.manacommunity.api.repository.AuctionPlayerRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
@RequiredArgsConstructor
public class AuctionCsvService {

    private final AuctionPlayerRepository playerRepo;
    private final AuctionConfigRepository configRepo;

    /**
     * CSV Format Expected:
     * player_name,category,role,age,base_price,matches,runs,wickets
     * Ravi Varma,BATSMEN,Right-Hand Bat,26,1000,42,1200,5
     * Ajay Kumar,BOWLERS,Right-Arm Fast,28,1000,38,200,52
     */
    public int uploadPlayersFromFile(Long configId, MultipartFile file) {
        AuctionConfig config = configRepo.findById(configId).orElseThrow();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            int queueOrder = playerRepo.findQueuedByConfig(configId).size() + 1;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; } // skip header

                String[] cols = line.split(",");
                if (cols.length < 5) continue;

                AuctionPlayer player = AuctionPlayer.builder()
                    .config(config)
                    .playerName(cols[0].trim())
                    .category(cols[1].trim().toUpperCase())
                    .playerRole(cols.length > 2 ? cols[2].trim() : null)
                    .age(cols.length > 3 ? Integer.parseInt(cols[3].trim()) : null)
                    .basePrice(cols.length > 4 ? Integer.parseInt(cols[4].trim()) : config.getBasePrice())
                    .statsJson(buildStats(cols))
                    .queueOrder(queueOrder++)
                    .status(AuctionPlayer.PlayerStatus.QUEUED)
                    .build();

                playerRepo.save(player);
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse player CSV: " + e.getMessage(), e);
        }
        return count;
    }

    private String buildStats(String[] cols) {
        if (cols.length < 8) return "{}";
        return String.format(
            "{\"matches\":%s,\"runs\":%s,\"wickets\":%s}",
            cols[5].trim(), cols[6].trim(), cols[7].trim()
        );
    }
}
