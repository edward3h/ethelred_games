package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionCardType {
    // Movement actions
    GO_TO_START("goToStart", 0, false),
    GO_TO_NEAREST_SCORE("goToNearestScore", 0, false),
    GO_TO_LOSE("goToLose", 0, false),  // Also used as fixed board action

    // Re-roll action
    ROLL_AGAIN("rollAgain", 0, false),

    // Card swap action
    EXCHANGE_CARDS("exchangeCards", 0, false),

    // Ahead movement
    AHEAD_1("ahead1", 1, true),
    AHEAD_2("ahead2", 2, true),
    AHEAD_3("ahead3", 3, true),
    AHEAD_4("ahead4", 4, true),
    AHEAD_5("ahead5", 5, true),
    AHEAD_6("ahead6", 6, true),
    AHEAD_10("ahead10", 10, true),
    AHEAD_12("ahead12", 12, true),

    // Back movement
    BACK_1("back1", -1, true),
    BACK_2("back2", -2, true),
    BACK_3("back3", -3, true),
    BACK_4("back4", -4, true),
    BACK_5("back5", -5, true),
    BACK_6("back6", -6, true),
    BACK_10("back10", -10, true),
    BACK_12("back12", -12, true),

    // Fixed board action only (not in deck)
    BACK_15("back15", -15, true);

    private final String code;
    private final int movement;
    private final boolean isMovementAction;

    ActionCardType(String code, int movement, boolean isMovementAction) {
        this.code = code;
        this.movement = movement;
        this.isMovementAction = isMovementAction;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public int getMovement() {
        return movement;
    }

    public boolean isMovementAction() {
        return isMovementAction;
    }

    public boolean isInDeck() {
        // BACK_15 and GO_TO_LOSE are only on the board, not in the deck
        // But GO_TO_LOSE cards exist as "Lose cards" which are different
        return this != BACK_15 && this != GO_TO_LOSE;
    }

    @JsonCreator
    public static ActionCardType fromCode(String code) {
        for (ActionCardType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown action card type: " + code);
    }
}
