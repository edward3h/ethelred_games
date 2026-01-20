package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.Player;

public class PassLoseInterruptPerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "passLoseInterrupt";

    private final RollDicePerformer rollDicePerformer;

    public PassLoseInterruptPerformer(RollDicePerformer rollDicePerformer) {
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
        validate(game.canPlayerInterrupt(player), "You cannot pass (already passed or not eligible)");

        // Mark this player as having passed
        game.playerPassedInterrupt(player);

        // Check if all players have responded
        if (game.allPlayersRespondedToInterrupt()) {
            // Close the interrupt window and continue the turn
            game.clearInterruptWindow();
            rollDicePerformer.continueAfterInterrupt(game, game.currentPlayer());
        }
        // Otherwise, wait for other players to respond
    }
}
