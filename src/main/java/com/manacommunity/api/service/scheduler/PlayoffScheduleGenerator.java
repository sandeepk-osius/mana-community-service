package com.manacommunity.api.service.scheduler;

import com.manacommunity.api.dto.scheduler.PlayoffGenerateRequest;
import com.manacommunity.api.dto.scheduler.PlayoffMatchDraftResponse;
import com.manacommunity.api.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless generator for playoff ("rounds to final") brackets.
 *
 * Faithful Java port of the frontend's playoffSchedule.ts so the server
 * reproduces the exact bracket the browser used to compute locally
 * (same pairings, round names, dates and 12-hour times).
 */
@Component
public class PlayoffScheduleGenerator {

    private static final Random RANDOM = new Random();

    // ── Slot / participant helpers ────────────────────────────────────

    /** A group proceeder slot (e.g. "Winner 1 of Group 2"). */
    private record Slot(int groupIndex, int rank) {}

    private static String proceederLabel(int groupIndex, int rank) {
        return "Winner " + rank + " of Group " + (groupIndex + 1);
    }

    private static String matchWinnerLabel(String matchName) {
        return matchName + " (Winner)";
    }

    private static String slotId(int groupIndex, int rank) {
        return "G" + (groupIndex + 1) + "-W" + rank;
    }

    private static List<Slot> buildProceederSlots(int numGroups, int proceedersPerGroup) {
        List<Slot> slots = new ArrayList<>();
        for (int g = 0; g < numGroups; g++) {
            for (int r = 1; r <= proceedersPerGroup; r++) {
                slots.add(new Slot(g, r));
            }
        }
        return slots;
    }

    private static <T> List<T> shuffle(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, RANDOM);
        return copy;
    }

    /**
     * Randomly chooses the index of the player to receive the BYE this round.
     * Prefers players who have not had a bye yet so the bye is shuffled fairly
     * across rounds; if every player in the round has already had one, it picks
     * randomly from the whole round.
     */
    private static int pickByeIndex(List<ParticipantRef> roundParticipants, Set<String> byeReceived) {
        List<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < roundParticipants.size(); i++) {
            if (!byeReceived.contains(roundParticipants.get(i).id())) {
                eligible.add(i);
            }
        }
        if (eligible.isEmpty()) {
            for (int i = 0; i < roundParticipants.size(); i++) {
                eligible.add(i);
            }
        }
        return eligible.get(RANDOM.nextInt(eligible.size()));
    }

    private static ParticipantRef toSlotParticipant(Slot slot) {
        return new ParticipantRef(slotId(slot.groupIndex(), slot.rank()),
                proceederLabel(slot.groupIndex(), slot.rank()));
    }

    private static ParticipantRef winnerRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-winner", matchWinnerLabel(matchName));
    }

    private static ParticipantRef loserRef(String matchId, String matchName) {
        return new ParticipantRef(matchId + "-loser", "Loser Of " + matchName);
    }

    // ── Pairing logic ─────────────────────────────────────────────────

    /** First-round pairings from group proceeder slots. */
    static List<Slot[]> buildFirstRoundPairings(List<Slot> slots, String seedingOrder, int numGroups) {
        if (slots.size() < 2) return new ArrayList<>();

        if ("SEQUENTIAL".equals(seedingOrder) && numGroups >= 2) {
            List<Slot[]> pairs = new ArrayList<>();
            for (int g = 0; g < numGroups; g += 2) {
                final int gi = g;
                List<Slot> left  = slots.stream().filter(s -> s.groupIndex() == gi).toList();
                List<Slot> right = slots.stream().filter(s -> s.groupIndex() == gi + 1).toList();
                int len = Math.min(left.size(), right.size());
                for (int i = 0; i < len; i++) {
                    pairs.add(new Slot[]{left.get(i), right.get(i)});
                }
            }
            if (!pairs.isEmpty()) return pairs;
        }

        List<Slot> ordered = new ArrayList<>(slots);
        if ("RANDOM".equals(seedingOrder)) {
            ordered = shuffle(ordered);
        }

        // TRADITIONAL (and RANDOM after shuffle): 1 vs N, 2 vs N-1 …
        List<Slot[]> pairs = new ArrayList<>();
        int lo = 0;
        int hi = ordered.size() - 1;
        while (lo < hi) {
            pairs.add(new Slot[]{ordered.get(lo), ordered.get(hi)});
            lo++;
            hi--;
        }
        return pairs;
    }

    // ── Round naming ──────────────────────────────────────────────────

    static String getRoundName(int roundIndex, int totalRounds) {
        if (roundIndex == totalRounds - 1) return "FINAL";
        if (roundIndex == totalRounds - 2) return "SEMI_FINAL";
        if (roundIndex == totalRounds - 3) return "QUARTER_FINAL";
        return "Round " + (roundIndex + 1);
    }

    private static String roundLabel(String type, int indexInRound) {
        return switch (type) {
            case "QUARTER_FINAL" -> "Quarter Final " + (indexInRound + 1);
            case "SEMI_FINAL"    -> "Semi-Final " + (indexInRound + 1);
            case "FINAL"         -> "Final";
            case "THIRD_PLACE"   -> "Third Place";
            default              -> type + " Match " + (indexInRound + 1);
        };
    }

    // ── 12-hour time helpers (must match playoffSchedule.ts exactly) ───

    private static final Pattern TIME_12H =
            Pattern.compile("^(\\d{1,2}):(\\d{2})\\s*(AM|PM)$", Pattern.CASE_INSENSITIVE);

    /** {hours, minutes}; falls back to 08:00 when unparseable (mirrors TS). */
    static int[] parseTime12h(String time) {
        if (time == null) return new int[]{8, 0};
        Matcher m = TIME_12H.matcher(time.trim());
        if (!m.matches()) return new int[]{8, 0};
        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        String meridiem = m.group(3).toUpperCase();
        if (meridiem.equals("PM") && hours != 12) hours += 12;
        if (meridiem.equals("AM") && hours == 12) hours = 0;
        return new int[]{hours, minutes};
    }

    static String formatTime12h(int hours, int minutes) {
        String meridiem = hours >= 12 ? "PM" : "AM";
        int h = hours % 12;
        if (h == 0) h = 12;
        return pad2(h) + ":" + pad2(minutes) + " " + meridiem;
    }

    static String addMinutesToTime(String time, int minutesToAdd) {
        int[] hm = parseTime12h(time);
        int total = hm[0] * 60 + hm[1] + minutesToAdd;
        int newHours = (total / 60) % 24;
        int newMinutes = total % 60;
        return formatTime12h(newHours, newMinutes);
    }

    static String addDaysToDateString(String dateStr, int days) {
        String[] parts = dateStr.split("-");
        LocalDate d = parts.length == 3
                ? LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))
                : LocalDate.parse(dateStr);
        return d.plusDays(days).toString();
    }

    private static String pad2(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    /** Sequential time/date cursor; rolls to next day when the slot passes midnight (hour < 6). */
    private static final class ScheduleCursor {
        private String slotDate;
        private String slotTime;
        private final String dayStartTime;
        private final int step;

        ScheduleCursor(String startDate, String startTime, int matchDurationMinutes, int breakMinutes) {
            this.slotDate = startDate;
            this.slotTime = startTime;
            this.dayStartTime = startTime;
            this.step = matchDurationMinutes + breakMinutes;
        }

        String[] next() {
            String[] current = {slotDate, slotTime};
            slotTime = addMinutesToTime(slotTime, step);
            if (parseTime12h(slotTime)[0] < 6) {
                slotDate = addDaysToDateString(slotDate, 1);
                slotTime = dayStartTime;
            }
            return current;
        }
    }

    // ── Bracket builders ──────────────────────────────────────────────

    /** Standard 2 groups × 2 proceeders → cross semi-finals + final. */
    private static List<PlayoffMatchDraftResponse> buildTwoGroupTwoProceederBracket(PlayoffGenerateRequest in) {
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.matchDurationMinutes(), in.breakMinutes());

        Slot[] sf1Pair;
        Slot[] sf2Pair;
        if ("SEQUENTIAL".equals(in.seedingOrder())) {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 1)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 2)};
        } else {
            sf1Pair = new Slot[]{new Slot(0, 1), new Slot(1, 2)};
            sf2Pair = new Slot[]{new Slot(0, 2), new Slot(1, 1)};
        }

        String sf1Id = "playoff-sf1";
        String sf2Id = "playoff-sf2";
        String finalId = "playoff-final";
        String[] s1 = cursor.next();
        String[] s2 = cursor.next();
        String[] sFinal = cursor.next();

        List<PlayoffMatchDraftResponse> matches = new ArrayList<>();
        matches.add(new PlayoffMatchDraftResponse(sf1Id, "Semi-Final 1", "SEMI_FINAL", 0,
                toSlotParticipant(sf1Pair[0]), toSlotParticipant(sf1Pair[1]),
                s1[0], s1[1], in.matchDurationMinutes(), in.venue(), in.court(), false));
        matches.add(new PlayoffMatchDraftResponse(sf2Id, "Semi-Final 2", "SEMI_FINAL", 0,
                toSlotParticipant(sf2Pair[0]), toSlotParticipant(sf2Pair[1]),
                s2[0], s2[1], in.matchDurationMinutes(), in.venue(), in.court(), false));
        matches.add(new PlayoffMatchDraftResponse(finalId, "Final", "FINAL", 1,
                winnerRef(sf1Id, "Semi-Final 1"), winnerRef(sf2Id, "Semi-Final 2"),
                sFinal[0], sFinal[1], in.matchDurationMinutes(), in.venue(), in.court(), false));

        if (in.thirdPlaceMatch()) {
            String[] s3 = cursor.next();
            matches.add(new PlayoffMatchDraftResponse("playoff-third-place", "Third Place", "THIRD_PLACE", 2,
                    loserRef(sf1Id, "Semi-Final 1"), loserRef(sf2Id, "Semi-Final 2"),
                    s3[0], s3[1], in.matchDurationMinutes(), in.venue(), in.court(), false));
        }
        return matches;
    }

    public List<PlayoffMatchDraftResponse> buildPlayoffBracket(PlayoffGenerateRequest in) {
        if (in.numGroups() == 2 && in.proceedersPerGroup() == 2) {
            return buildTwoGroupTwoProceederBracket(in);
        }

        List<Slot> slots = buildProceederSlots(in.numGroups(), in.proceedersPerGroup());
        if (slots.size() < 2) return new ArrayList<>();

        List<Slot[]> firstRoundPairs = buildFirstRoundPairings(slots, in.seedingOrder(), in.numGroups());
        List<ParticipantRef> current = new ArrayList<>();
        for (Slot[] pair : firstRoundPairs) {
            current.add(toSlotParticipant(pair[0]));
            current.add(toSlotParticipant(pair[1]));
        }

        // Add any unpaired leftover slots (the BYEs in round 1)
        Set<String> pairedIds = new HashSet<>();
        current.forEach(p -> pairedIds.add(p.id()));
        for (Slot slot : slots) {
            ParticipantRef p = toSlotParticipant(slot);
            if (!pairedIds.contains(p.id())) {
                current.add(p);
            }
        }

        // Simulate round sizes to find total rounds
        int simLength = current.size();
        int totalRounds = 0;
        while (simLength > 1) {
            totalRounds++;
            simLength = (int) Math.ceil(simLength / 2.0);
        }

        List<PlayoffMatchDraftResponse> matches = new ArrayList<>();
        int roundIndex = 0;
        List<ParticipantRef> roundParticipants = new ArrayList<>(current);
        ScheduleCursor cursor = new ScheduleCursor(
                in.startDate(), in.startTime(), in.matchDurationMinutes(), in.breakMinutes());

        // Track who has already received a BYE so the bye rotates fairly across
        // rounds (a player should not get a bye again until everyone else has).
        Set<String> byeReceived = new HashSet<>();

        while (roundParticipants.size() > 1) {
            String roundType = getRoundName(roundIndex, totalRounds);
            List<ParticipantRef> nextParticipants = new ArrayList<>();
            int len = roundParticipants.size();

            if (len % 2 != 0) {
                // Randomly pick the BYE among players who have NOT had one yet;
                // only if everyone in this round already has, fall back to all.
                int byeIdx = pickByeIndex(roundParticipants, byeReceived);
                ParticipantRef byeParticipant = roundParticipants.get(byeIdx);
                byeReceived.add(byeParticipant.id());

                List<ParticipantRef> activeParticipants = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    if (i != byeIdx) activeParticipants.add(roundParticipants.get(i));
                }
                int numMatches = activeParticipants.size() / 2;

                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = activeParticipants.get(2 * mIdx);
                    ParticipantRef away = activeParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new PlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venue(), in.court(), false));
                    nextParticipants.add(winnerRef(id, name));
                }

                // Generate the BYE match at the end of the round
                int byeMatchIdx = numMatches;
                String byeMatchId = "playoff-r" + roundIndex + "-m" + byeMatchIdx;
                String byeMatchName = byeParticipant.name() + " (BYE)";
                String[] slot = cursor.next();
                matches.add(new PlayoffMatchDraftResponse(byeMatchId, byeMatchName, roundType, roundIndex,
                        byeParticipant, new ParticipantRef("bye", "BYE"), slot[0], slot[1],
                        in.matchDurationMinutes(), in.venue(), in.court(), false));

                // Advance the BYE player directly (preserving clean name)
                nextParticipants.add(byeParticipant);
            } else {
                int numMatches = len / 2;
                for (int mIdx = 0; mIdx < numMatches; mIdx++) {
                    ParticipantRef home = roundParticipants.get(2 * mIdx);
                    ParticipantRef away = roundParticipants.get(2 * mIdx + 1);
                    String id = "playoff-r" + roundIndex + "-m" + mIdx;
                    String name = roundLabel(roundType, mIdx);
                    String[] slot = cursor.next();
                    matches.add(new PlayoffMatchDraftResponse(id, name, roundType, roundIndex,
                            home, away, slot[0], slot[1],
                            in.matchDurationMinutes(), in.venue(), in.court(), false));
                    nextParticipants.add(winnerRef(id, name));
                }
            }

            roundParticipants = nextParticipants;
            roundIndex++;
        }

        if (in.thirdPlaceMatch()) {
            List<PlayoffMatchDraftResponse> semiMatches =
                    matches.stream().filter(m -> "SEMI_FINAL".equals(m.round())).toList();
            if (semiMatches.size() >= 2) {
                String[] slot = cursor.next();
                matches.add(new PlayoffMatchDraftResponse("playoff-third-place", "Third Place",
                        "THIRD_PLACE", roundIndex,
                        loserRef(semiMatches.get(0).id(), semiMatches.get(0).name()),
                        loserRef(semiMatches.get(1).id(), semiMatches.get(1).name()),
                        slot[0], slot[1], in.matchDurationMinutes(), in.venue(), in.court(), false));
            }
        }

        return matches;
    }
}
