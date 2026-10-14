package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethelred.games.core.GamePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BonkersPlayer implements GamePlayer {
    private BoardPosition position = BoardPosition.START;
    private int score = 0;
    private boolean hasLoseCard = true;
    private final List<ActionCard> actionCards = new ArrayList<>();

    public BonkersPlayer() {
    }

    public BoardPosition getPosition() {
        return position;
    }

    public void setPosition(BoardPosition position) {
        this.position = position;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        score = Math.max(0, Math.min(12, score + delta));
    }

    public boolean hasWon() {
        return score >= 12;
    }

    public boolean hasLoseCard() {
        return hasLoseCard;
    }

    public void useLoseCard() {
        hasLoseCard = false;
    }

    @JsonProperty("actionCards")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<ActionCard> getActionCards() {
        return actionCards;
    }

    public void giveCard(ActionCard card) {
        actionCards.add(card);
    }

    public ActionCard takeCard(ActionCard card) {
        if (actionCards.remove(card)) {
            return card;
        }
        return null;
    }

    public ActionCard findCardByCode(String cardCode) {
        return actionCards.stream()
                .filter(c -> c.shortCode().equals(cardCode))
                .findFirst()
                .orElse(null);
    }

    public boolean hasActionCards() {
        return !actionCards.isEmpty();
    }

    @Override
    public Map<String, Object> publicProperties() {
        return Map.of(
                "position", position.index(),
                "score", score,
                "cardCount", actionCards.size(),
                "hasLoseCard", hasLoseCard
        );
    }
}
