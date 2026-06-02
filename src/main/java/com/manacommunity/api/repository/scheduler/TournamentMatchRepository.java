package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {

    List<TournamentMatch> findByConfigIdOrderByScheduledAt(Long configId);

    List<TournamentMatch> findByConfigId(Long configId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.config.id=:cid AND m.roundNumber=:rn ORDER BY m.matchNumber")
    List<TournamentMatch> findByConfigIdAndRoundNumberOrderByMatchNumber(
        @Param("cid") Long configId, @Param("rn") int roundNumber);

    @Query("SELECT MAX(m.swissRoundNumber) FROM TournamentMatch m WHERE m.config.id=:cid")
    Optional<Integer> findMaxSwissRound(@Param("cid") Long configId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.config.id=:cid ORDER BY m.swissRoundNumber,m.matchNumber")
    List<TournamentMatch> findByConfigIdOrderBySwissRoundNumber(@Param("cid") Long configId);

    @Query("SELECT m FROM TournamentMatch m WHERE m.config.id=:cid AND m.status='SCHEDULED' ORDER BY m.scheduledAt")
    List<TournamentMatch> findUpcoming(@Param("cid") Long configId);
}
