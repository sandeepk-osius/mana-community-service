package com.manacommunity.api.service;

import com.manacommunity.api.dto.TournamentRequest;
import com.manacommunity.api.model.SportsEvent;
import com.manacommunity.api.model.Tournament;
import java.util.List;

public interface TournamentService {
    List<Tournament> getAllTournaments();
    List<Tournament> getCommunityTournaments(Long communityId);
    Tournament getTournamentById(Long id);
    void deleteTournament(Long id);
    Tournament saveTournamentRecord(TournamentRequest req, Boolean allowAdminChat);
    Tournament updateStatus(Long id, String status);
}
