package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.InvalidActionException;
import org.ethelred.games.core.Player;

public class ChooseExchangeCardPerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "chooseExchangeCard";

    private final RollDicePerformer rollDicePerformer;

    public ChooseExchangeCardPerformer(RollDicePerformer rollDicePerformer) {
        this.rollDicePerformer = rollDicePerformer;
    }

    @Override
    public String actionName() {
        return NAME;
    }

    @Override
    public void perform(BonkersGame game, Player player, Action action) {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.getTurnPhase() == TurnPhase.WAITING_FOR_EXCHANGE_CHOICE, "Not waiting for exchange choice");
        validate(game.currentPlayer().same(player), "Not your turn");

        BoardPosition targetPosition;
        try {
            targetPosition = BoardPosition.of(Integer.parseInt(action.argumentAsString()));
        } catch (NumberFormatException e) {
            throw new InvalidActionException("Invalid position: " + action.argumentAsString());
        }

        BoardPosition sourcePosition = game.getExchangeCardSource();
        validate(sourcePosition != null, "No source position for exchange");
        validate(!targetPosition.equals(sourcePosition), "Cannot exchange card with itself");

        // Get the cards at both positions
        ActionCard sourceCard = game.getCardOnSpace(sourcePosition);
        ActionCard targetCard = game.getCardOnSpace(targetPosition);

        validate(sourceCard != null, "No card at source position");
        validate(targetCard != null, "No card at target position");
        validate(sourceCard.type() == ActionCardType.EXCHANGE_CARDS, "Source card is not Exchange Cards");

        // Swap the cards and finish the exchange
        game.completeExchange(targetPosition);

        // The player hasn't moved, but we need to continue processing
        // The new card is now at the source position (where the player is)
        // We need to re-check from step 3 (score/lose check)
        rollDicePerformer.processLanding(game, player);
    }
}
