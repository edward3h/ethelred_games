package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.Player;

public class PlayLoseCardPerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "playLoseCard";

    private final RollDicePerformer rollDicePerformer;

    public PlayLoseCardPerformer(RollDicePerformer rollDicePerformer) {
        this.rollDicePerformer = rollDicePerformer;
    }

    @Override
    public String actionName() {
        return NAME;
    }

    @Override
    public void perform(BonkersGame game, Player player, Action action) {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(!game.currentPlayer().same(player), "Cannot play Lose card on yourself");
        validate(game.canPlayLoseCard(player), "You don't have a Lose card");

        game.gamePlayer(player).useLoseCard();

        // The active player's turn stops immediately: they move to the Lose space, lose a point and pass the dice
        Player activePlayer = game.currentPlayer();
        BonkersPlayer activeBp = game.gamePlayer(activePlayer);
        activeBp.setPosition(BoardPosition.LOSE);
        activeBp.addScore(-1);
        rollDicePerformer.endTurn(game);
    }
}
