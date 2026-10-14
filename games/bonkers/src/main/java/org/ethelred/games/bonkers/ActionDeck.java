package org.ethelred.games.bonkers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ActionDeck {
    private final List<ActionCard> cards;
    private int nextCardIndex = 0;

    public ActionDeck(Random random) {
        cards = createDeck();
        Collections.shuffle(cards, random);
    }

    private List<ActionCard> createDeck() {
        List<ActionCard> deck = new ArrayList<>(36);
        int id = 0;

        // 2 of each action card type (except BACK_15 and GO_TO_LOSE which are board-only)
        for (ActionCardType type : ActionCardType.values()) {
            if (type.isInDeck()) {
                deck.add(new ActionCard(type, id++));
                deck.add(new ActionCard(type, id++));
            }
        }

        return deck;
    }

    public ActionCard draw() {
        if (!hasCards()) {
            return null;
        }
        return cards.get(nextCardIndex++);
    }

    public boolean hasCards() {
        return nextCardIndex < cards.size();
    }

    public int remainingCards() {
        return cards.size() - nextCardIndex;
    }
}
