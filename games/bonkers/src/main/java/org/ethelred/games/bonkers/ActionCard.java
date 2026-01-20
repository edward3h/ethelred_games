package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record ActionCard(ActionCardType type, int id) implements Comparable<ActionCard> {

    @JsonValue
    public String shortCode() {
        return type.getCode() + "_" + id;
    }

    @JsonCreator
    public static ActionCard fromCode(String code) {
        int underscoreIndex = code.lastIndexOf('_');
        if (underscoreIndex < 0) {
            throw new IllegalArgumentException("Invalid action card code: " + code);
        }
        String typeCode = code.substring(0, underscoreIndex);
        int id = Integer.parseInt(code.substring(underscoreIndex + 1));
        return new ActionCard(ActionCardType.fromCode(typeCode), id);
    }

    @Override
    public int compareTo(ActionCard other) {
        int typeCompare = type.compareTo(other.type);
        if (typeCompare != 0) {
            return typeCompare;
        }
        return Integer.compare(id, other.id);
    }

    public String displayName() {
        return switch (type) {
            case GO_TO_START -> "Go To Start";
            case GO_TO_NEAREST_SCORE -> "Go To Nearest Score";
            case GO_TO_LOSE -> "Go To Lose";
            case ROLL_AGAIN -> "Roll Again";
            case EXCHANGE_CARDS -> "Exchange Cards";
            case AHEAD_1 -> "Ahead 1";
            case AHEAD_2 -> "Ahead 2";
            case AHEAD_3 -> "Ahead 3";
            case AHEAD_4 -> "Ahead 4";
            case AHEAD_5 -> "Ahead 5";
            case AHEAD_6 -> "Ahead 6";
            case AHEAD_10 -> "Ahead 10";
            case AHEAD_12 -> "Ahead 12";
            case BACK_1 -> "Back 1";
            case BACK_2 -> "Back 2";
            case BACK_3 -> "Back 3";
            case BACK_4 -> "Back 4";
            case BACK_5 -> "Back 5";
            case BACK_6 -> "Back 6";
            case BACK_10 -> "Back 10";
            case BACK_12 -> "Back 12";
            case BACK_15 -> "Back 15";
        };
    }
}
