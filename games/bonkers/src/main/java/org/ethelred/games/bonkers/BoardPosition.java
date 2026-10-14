package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * A space on the board loop. Positions always wrap into the range 0 to {@link #SPACES} - 1, and
 * serialise to JSON as that plain number.
 */
public record BoardPosition(@JsonValue int index) implements Comparable<BoardPosition> {
    public static final int SPACES = 55;

    public static final BoardPosition START = new BoardPosition(0);
    public static final BoardPosition LOSE = new BoardPosition(31);

    private static final List<BoardPosition> SCORE_SPACES = List.of(START, new BoardPosition(12), new BoardPosition(24));

    // Permanent actions printed on the board, as opposed to action cards placed by players
    private static final Map<BoardPosition, ActionCardType> FIXED_ACTIONS = Map.of(
            new BoardPosition(35), ActionCardType.GO_TO_LOSE,
            new BoardPosition(43), ActionCardType.BACK_15,
            new BoardPosition(49), ActionCardType.GO_TO_LOSE);

    /**
     * The nearest Score space in each direction. When both are the same distance away the player chooses.
     */
    public record NearestScores(
            BoardPosition forward, int forwardDistance, BoardPosition backward, int backwardDistance) {
        public boolean isTie() {
            return forwardDistance == backwardDistance;
        }

        public BoardPosition nearest() {
            if (isTie()) {
                throw new IllegalStateException("Nearest score is a tie");
            }
            return forwardDistance < backwardDistance ? forward : backward;
        }
    }

    public BoardPosition {
        index = Math.floorMod(index, SPACES);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BoardPosition of(int index) {
        return new BoardPosition(index);
    }

    public BoardPosition plus(int spaces) {
        return new BoardPosition(index + spaces);
    }

    public SpaceType spaceType() {
        if (equals(START)) {
            return SpaceType.START_SCORE;
        }
        if (equals(LOSE)) {
            return SpaceType.LOSE;
        }
        if (SCORE_SPACES.contains(this)) {
            return SpaceType.SCORE;
        }
        if (FIXED_ACTIONS.containsKey(this)) {
            return SpaceType.FIXED_ACTION;
        }
        return SpaceType.BLANK;
    }

    /** Start also counts as a Score space. */
    public boolean isScore() {
        return SCORE_SPACES.contains(this);
    }

    public boolean isLose() {
        return equals(LOSE);
    }

    public boolean isBlank() {
        return spaceType() == SpaceType.BLANK;
    }

    /** The permanent action on this space, or null if there is none. */
    public ActionCardType fixedAction() {
        return FIXED_ACTIONS.get(this);
    }

    /** Nearest Score space in each direction, not counting this space itself. */
    public NearestScores nearestScores() {
        BoardPosition forward = null;
        BoardPosition backward = null;
        int forwardDistance = Integer.MAX_VALUE;
        int backwardDistance = Integer.MAX_VALUE;
        for (var score : SCORE_SPACES) {
            int ahead = Math.floorMod(score.index - index, SPACES);
            int behind = Math.floorMod(index - score.index, SPACES);
            // Distance 0 is this space; going all the way round to it doesn't count as nearest
            if (ahead == 0) {
                ahead = SPACES;
            }
            if (behind == 0) {
                behind = SPACES;
            }
            if (ahead < forwardDistance) {
                forwardDistance = ahead;
                forward = score;
            }
            if (behind < backwardDistance) {
                backwardDistance = behind;
                backward = score;
            }
        }
        return new NearestScores(forward, forwardDistance, backward, backwardDistance);
    }

    @Override
    public int compareTo(BoardPosition other) {
        return Integer.compare(index, other.index);
    }

    @Override
    public String toString() {
        return String.valueOf(index);
    }
}
