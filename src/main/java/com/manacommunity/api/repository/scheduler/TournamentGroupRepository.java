package com.manacommunity.api.repository.scheduler;

import com.manacommunity.api.model.scheduler.TournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, Long> {
    List<TournamentGroup> findByConfigIdOrderByGroupOrder(Long configId);
}
