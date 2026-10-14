package org.ethelred.games.bonkers;

import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.BaseGameDefinition;
import org.ethelred.games.util.Util;

import java.util.Set;

public class BonkersGameDefinition extends BaseGameDefinition<BonkersGame> {

    public static final String BONKERS = "bonkers";

    @Override
    public String gameType() {
        return BONKERS;
    }

    @Override
    public Set<ActionPerformer<? super BonkersGame>> actionPerformers() {
        // Create performers with dependencies
        var rollDice = new RollDicePerformer();
        var placeCard = new PlaceCardPerformer(rollDice);
        var chooseExchange = new ChooseExchangeCardPerformer(rollDice);
        var chooseDirection = new ChooseDirectionPerformer(rollDice);
        var playLoseCard = new PlayLoseCardPerformer(rollDice);

        return Util.merge(
                Set.of(
                        rollDice,
                        placeCard,
                        chooseExchange,
                        chooseDirection,
                        playLoseCard
                ),
                super.actionPerformers()
        );
    }

    @Override
    public BonkersGame create(long id) {
        return new BonkersGame(id);
    }
}
