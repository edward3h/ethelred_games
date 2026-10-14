package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TurnPhase {
    WAITING_FOR_ROLL("waitingForRoll"),
    WAITING_FOR_CARD_PLACEMENT("waitingForCardPlacement"),
    WAITING_FOR_EXCHANGE_CHOICE("waitingForExchangeChoice"),
    WAITING_FOR_DIRECTION("waitingForDirection");

    private final String code;

    TurnPhase(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
