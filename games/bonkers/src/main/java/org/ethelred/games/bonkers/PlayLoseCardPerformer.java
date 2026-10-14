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
        validate(game.getTurnPhase() == TurnPhase.WAITING_FOR_LOSE_INTERRUPT, "Not in interrupt window");
        validate(!game.currentPlayer().same(player), "Cannot play Lose card on yourself");
        validate(game.canPlayerInterrupt(player), "You cannot interrupt");

        BonkersPlayer interrupter = game.gamePlayer(player);
        validate(interrupter.hasLoseCard(), "You don't have a Lose card");

        // Use the Lose card
        interrupter.useLoseCard();

        // Clear the interrupt window - the interrupt happened
        game.clearInterruptWindow();
        // Leave the interrupt phase: processLanding only re-opens it if someone else can still interrupt
        game.setTurnPhase(TurnPhase.WAITING_FOR_ROLL);

        // Move the active player to the Lose space
        Player activePlayer = game.currentPlayer();
        BonkersPlayer activeBp = game.gamePlayer(activePlayer);
        activeBp.setPosition(Board.LOSE_POSITION);

        // Continue from step 3 for the active player (process landing at Lose space)
        rollDicePerformer.processLanding(game, activePlayer);
    }
}
