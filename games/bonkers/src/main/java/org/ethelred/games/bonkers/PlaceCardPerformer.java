package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.InvalidActionException;
import org.ethelred.games.core.Player;

public class PlaceCardPerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "placeCard";

    private final RollDicePerformer rollDicePerformer;

    public PlaceCardPerformer(RollDicePerformer rollDicePerformer) {
        this.rollDicePerformer = rollDicePerformer;
    }

    @Override
    public String actionName() {
        return NAME;
    }

    @Override
    public void perform(BonkersGame game, Player player, Action action) {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.getTurnPhase() == TurnPhase.WAITING_FOR_CARD_PLACEMENT, "Not waiting for card placement");
        validate(game.currentPlayer().same(player), "Not your turn");

        String cardCode = action.argumentAsString();
        BonkersPlayer bp = game.gamePlayer(player);

        // Find the card in player's hand
        ActionCard card = bp.findCardByCode(cardCode);
        if (card == null) {
            throw new InvalidActionException("You don't have that card: " + cardCode);
        }

        // Remove from player's hand
        bp.takeCard(card);

        // Place on current position
        int position = bp.getPosition();
        game.placeCardOnSpace(position, card);

        // Mark that we've placed a card this turn
        game.setHasPlacedCardThisTurn(true);

        // Draw a replacement card from deck if available
        ActionCard newCard = game.getDeck().draw();
        if (newCard != null) {
            bp.giveCard(newCard);
        }

        // Continue turn processing - check for actions on space
        // Note: The card we just placed is now on the space, so it will be executed
        rollDicePerformer.checkAndExecuteAction(game, player);
    }
}
