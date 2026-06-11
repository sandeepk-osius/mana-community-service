package com.manacommunity.api.unit.service;

import com.manacommunity.api.dto.scheduler.PlayoffGenerateRequest;
import com.manacommunity.api.dto.scheduler.PlayoffMatchDraftResponse;
import com.manacommunity.api.service.scheduler.PlayoffScheduleGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlayoffScheduleGenerator - rounds-to-final bracket")
class PlayoffScheduleGeneratorTest {

    private final PlayoffScheduleGenerator generator = new PlayoffScheduleGenerator();

    private PlayoffGenerateRequest request(int numGroups, int proceeders, String seeding,
                                           boolean thirdPlace) {
        return new PlayoffGenerateRequest(
                numGroups, proceeders, seeding, thirdPlace,
                "2026-06-20", "08:00 AM", 30, 10, "LE", "Court 1");
    }

    @Nested
    @DisplayName("2 groups x 2 proceeders")
    class TwoGroupTwoProceeder {

        @Test
        @DisplayName("produces cross semi-finals + final")
        void crossSemisAndFinal() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", false));

            assertThat(matches).hasSize(3);
            assertThat(matches).extracting(PlayoffMatchDraftResponse::round)
                    .containsExactly("SEMI_FINAL", "SEMI_FINAL", "FINAL");

            // TRADITIONAL cross seeding: SF1 = G1-W1 vs G2-W2, SF2 = G1-W2 vs G2-W1
            assertThat(matches.get(0).home().id()).isEqualTo("G1-W1");
            assertThat(matches.get(0).away().id()).isEqualTo("G2-W2");
            assertThat(matches.get(1).home().id()).isEqualTo("G1-W2");
            assertThat(matches.get(1).away().id()).isEqualTo("G2-W1");

            // Final is fed by the two semi-final winners
            assertThat(matches.get(2).home().id()).isEqualTo("playoff-sf1-winner");
            assertThat(matches.get(2).away().id()).isEqualTo("playoff-sf2-winner");
        }

        @Test
        @DisplayName("SEQUENTIAL seeding pairs same ranks across groups")
        void sequentialSeeding() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "SEQUENTIAL", false));

            assertThat(matches.get(0).home().id()).isEqualTo("G1-W1");
            assertThat(matches.get(0).away().id()).isEqualTo("G2-W1");
            assertThat(matches.get(1).home().id()).isEqualTo("G1-W2");
            assertThat(matches.get(1).away().id()).isEqualTo("G2-W2");
        }

        @Test
        @DisplayName("third-place flag adds a THIRD_PLACE match from semi losers")
        void thirdPlaceAdded() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", true));

            assertThat(matches).hasSize(4);
            PlayoffMatchDraftResponse third = matches.get(3);
            assertThat(third.round()).isEqualTo("THIRD_PLACE");
            assertThat(third.home().id()).isEqualTo("playoff-sf1-loser");
            assertThat(third.away().id()).isEqualTo("playoff-sf2-loser");
        }

        @Test
        @DisplayName("schedules matches sequentially with break between slots")
        void sequentialSlots() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(2, 2, "TRADITIONAL", false));

            // 30m duration + 10m break = 40m steps from 08:00 AM
            assertThat(matches.get(0).time()).isEqualTo("08:00 AM");
            assertThat(matches.get(1).time()).isEqualTo("08:40 AM");
            assertThat(matches.get(2).time()).isEqualTo("09:20 AM");
            assertThat(matches).allSatisfy(m -> {
                assertThat(m.date()).isEqualTo("2026-06-20");
                assertThat(m.venue()).isEqualTo("LE");
                assertThat(m.court()).isEqualTo("Court 1");
                assertThat(m.duration()).isEqualTo(30);
            });
        }
    }

    @Nested
    @DisplayName("general bracket")
    class GeneralBracket {

        @Test
        @DisplayName("4 groups x 1 proceeder => semis + final, names FINAL/SEMI_FINAL")
        void fourTeams() {
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(4, 1, "TRADITIONAL", false));

            assertThat(matches).extracting(PlayoffMatchDraftResponse::round)
                    .containsExactly("SEMI_FINAL", "SEMI_FINAL", "FINAL");
            // Final winner refs point at the semi matches generated this round
            assertThat(matches.get(2).home().id()).contains("winner");
            assertThat(matches.get(2).away().id()).contains("winner");
        }

        @Test
        @DisplayName("odd participant count produces a BYE match and advances the player")
        void oddCountByes() {
            // 3 groups x 1 proceeder = 3 participants -> one BYE in round 1
            List<PlayoffMatchDraftResponse> matches =
                    generator.buildPlayoffBracket(request(3, 1, "TRADITIONAL", false));

            assertThat(matches).anySatisfy(m -> assertThat(m.name()).contains("(BYE)"));
            // A bracket of 3 collapses to a final, so a FINAL must exist
            assertThat(matches).anySatisfy(m -> assertThat(m.round()).isEqualTo("FINAL"));
        }
    }

    @Nested
    @DisplayName("round naming")
    class RoundNaming {
        @Test
        @DisplayName("getRoundName maps last three rounds to FINAL/SEMI/QUARTER")
        void roundNames() {
            assertThat(PlayoffScheduleGenerator.getRoundName(2, 3)).isEqualTo("FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(1, 3)).isEqualTo("SEMI_FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(0, 3)).isEqualTo("QUARTER_FINAL");
            assertThat(PlayoffScheduleGenerator.getRoundName(0, 5)).isEqualTo("Round 1");
        }
    }

    @Nested
    @DisplayName("12-hour time helpers")
    class TimeHelpers {
        @Test
        @DisplayName("addMinutesToTime rolls across the hour and noon correctly")
        void addMinutes() {
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("08:00 AM", 40)).isEqualTo("08:40 AM");
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("11:50 AM", 20)).isEqualTo("12:10 PM");
            assertThat(PlayoffScheduleGenerator.addMinutesToTime("11:30 PM", 40)).isEqualTo("12:10 AM");
        }
    }
}
