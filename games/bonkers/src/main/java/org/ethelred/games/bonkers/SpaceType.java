package org.ethelred.games.bonkers;

public enum SpaceType {
    START_SCORE,   // Position 0 - acts as both Start and Score
    BLANK,         // Can have action cards placed on it
    SCORE,         // Positions 12, 24
    LOSE,          // Position 31
    FIXED_ACTION   // Positions 35, 43, 49 - permanent actions on board
}
