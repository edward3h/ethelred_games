package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TurnPhase {
    // Active player phases
    WAITING_FOR_ROLL("waitingForRoll"),
    WAITING_FOR_CARD_PLACEMENT("waitingForCardPlacement"),
    WAITING_FOR_EXCHANGE_CHOICE("waitingForExchangeChoice"),
    WAITING_FOR_DIRECTION("waitingForDirection"),

    // Interrupt phase - any player with Lose card can act
    WAITING_FOR_LOSE_INTERRUPT("waitingForLoseInterrupt");

    private final String code;

    TurnPhase(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
