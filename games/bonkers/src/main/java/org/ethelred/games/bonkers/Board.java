package org.ethelred.games.bonkers;

import java.util.Set;

public final class Board {
    public static final int TOTAL_SPACES = 55;

    // Special positions
    public static final int START_POSITION = 0;  // Also counts as Score
    public static final int LOSE_POSITION = 31;

    // Score positions (including Start)
    private static final Set<Integer> SCORE_POSITIONS = Set.of(0, 12, 24);

    // Fixed action positions (permanent actions on board, not from cards)
    public static final int GO_TO_LOSE_1 = 35;
    public static final int BACK_15 = 43;
    public static final int GO_TO_LOSE_2 = 49;
    private static final Set<Integer> FIXED_ACTION_POSITIONS = Set.of(GO_TO_LOSE_1, BACK_15, GO_TO_LOSE_2);

    private Board() {
        // Utility class
    }

    public static SpaceType getSpaceType(int position) {
        int pos = normalizePosition(position);
        if (pos == START_POSITION) {
            return SpaceType.START_SCORE;
        }
        if (pos == LOSE_POSITION) {
            return SpaceType.LOSE;
        }
        if (SCORE_POSITIONS.contains(pos)) {
            return SpaceType.SCORE;
        }
        if (FIXED_ACTION_POSITIONS.contains(pos)) {
            return SpaceType.FIXED_ACTION;
        }
        return SpaceType.BLANK;
    }

    public static boolean isScoreSpace(int position) {
        return SCORE_POSITIONS.contains(normalizePosition(position));
    }

    public static boolean isLoseSpace(int position) {
        return normalizePosition(position) == LOSE_POSITION;
    }

    public static boolean isBlankSpace(int position) {
        return getSpaceType(position) == SpaceType.BLANK;
    }

    public static ActionCardType getFixedAction(int position) {
        int pos = normalizePosition(position);
        if (pos == GO_TO_LOSE_1 || pos == GO_TO_LOSE_2) {
            return ActionCardType.GO_TO_LOSE;
        }
        if (pos == BACK_15) {
            return ActionCardType.BACK_15;
        }
        return null;
    }

    public static int normalizePosition(int position) {
        int pos = position % TOTAL_SPACES;
        return pos < 0 ? pos + TOTAL_SPACES : pos;
    }

    public static int move(int fromPosition, int spaces) {
        return normalizePosition(fromPosition + spaces);
    }

    public static int[] findNearestScorePositions(int fromPosition) {
        int pos = normalizePosition(fromPosition);

        // Find distance to each score space in both directions
        int[] scorePositions = {0, 12, 24};
        int minDistance = Integer.MAX_VALUE;
        int nearestForward = -1;
        int nearestBackward = -1;

        for (int scorePos : scorePositions) {
            // Forward distance
            int forwardDist = (scorePos - pos + TOTAL_SPACES) % TOTAL_SPACES;
            if (forwardDist == 0) forwardDist = TOTAL_SPACES; // Don't count current position

            // Backward distance
            int backwardDist = (pos - scorePos + TOTAL_SPACES) % TOTAL_SPACES;
            if (backwardDist == 0) backwardDist = TOTAL_SPACES;

            int minDist = Math.min(forwardDist, backwardDist);
            if (minDist < minDistance) {
                minDistance = minDist;
                if (forwardDist <= backwardDist) {
                    nearestForward = scorePos;
                    nearestBackward = -1;
                } else {
                    nearestBackward = scorePos;
                    nearestForward = -1;
                }
            } else if (minDist == minDistance) {
                // Tie - we have options in both directions
                if (forwardDist == minDistance) {
                    nearestForward = scorePos;
                }
                if (backwardDist == minDistance) {
                    nearestBackward = scorePos;
                }
            }
        }

        if (nearestForward >= 0 && nearestBackward >= 0 && nearestForward != nearestBackward) {
            return new int[]{nearestForward, nearestBackward};
        } else if (nearestForward >= 0) {
            return new int[]{nearestForward};
        } else {
            return new int[]{nearestBackward};
        }
    }
}
