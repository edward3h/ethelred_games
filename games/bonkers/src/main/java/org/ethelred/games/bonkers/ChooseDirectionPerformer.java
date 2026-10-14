package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.InvalidActionException;
import org.ethelred.games.core.Player;

public class ChooseDirectionPerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "chooseDirection";

    private final RollDicePerformer rollDicePerformer;

    public ChooseDirectionPerformer(RollDicePerformer rollDicePerformer) {
        this.rollDicePerformer = rollDicePerformer;
    }

    @Override
    public String actionName() {
        return NAME;
    }

    @Override
    public void perform(BonkersGame game, Player player, Action action) {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.getTurnPhase() == TurnPhase.WAITING_FOR_DIRECTION, "Not waiting for direction choice");
        validate(game.currentPlayer().same(player), "Not your turn");

        var options = game.getPendingScores();
        validate(options != null, "No direction options available");

        String direction = action.argumentAsString();
        BoardPosition targetPosition;

        if ("forward".equals(direction)) {
            targetPosition = options.forward();
        } else if ("backward".equals(direction)) {
            targetPosition = options.backward();
        } else {
            throw new InvalidActionException("Invalid direction: " + direction + ". Must be 'forward' or 'backward'");
        }

        // Move player to chosen Score space
        BonkersPlayer bp = game.gamePlayer(player);
        bp.setPosition(targetPosition);

        // Clear pending options
        game.setPendingScores(null);

        // Continue processing from landing
        rollDicePerformer.processLanding(game, player);
    }
}
