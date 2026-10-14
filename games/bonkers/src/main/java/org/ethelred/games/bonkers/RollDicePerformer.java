package org.ethelred.games.bonkers;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.Player;

public class RollDicePerformer implements ActionPerformer<BonkersGame> {
    public static final String NAME = "rollDice";

    @Override
    public String actionName() {
        return NAME;
    }

    @Override
    public void perform(BonkersGame game, Player player, Action action) {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.getTurnPhase() == TurnPhase.WAITING_FOR_ROLL, "Not waiting for roll");
        validate(game.currentPlayer().same(player), "Not your turn");

        BonkersPlayer bp = game.gamePlayer(player);

        // Step 1: Roll the dice
        int roll = game.rollDice();

        // Step 2: If rolled 12, score a point
        if (roll == 12) {
            bp.addScore(1);
            if (bp.hasWon()) {
                game.winner(player);
                return;
            }
        }

        // Move the player
        bp.setPosition(bp.getPosition().plus(roll));

        // Process the landing
        processLanding(game, player);
    }

    /**
     * Process landing on a space. This handles steps 3-8 of the turn flow.
     * Can be called recursively when actions cause movement.
     */
    public void processLanding(BonkersGame game, Player player) {
        BonkersPlayer bp = game.gamePlayer(player);
        BoardPosition position = bp.getPosition();

        // Step 3: Check for Score space
        if (position.isScore()) {
            bp.addScore(1);
            if (bp.hasWon()) {
                game.winner(player);
                return;
            }
        }

        // Step 4: Check for Lose space
        if (position.isLose()) {
            bp.addScore(-1);
        }

        // Open interrupt window for Lose cards
        game.openLoseInterruptWindow();
        if (game.getTurnPhase() == TurnPhase.WAITING_FOR_LOSE_INTERRUPT) {
            // Interrupt window is open, wait for players to respond
            return;
        }

        // Continue processing
        continueAfterInterrupt(game, player);
    }

    /**
     * Continue turn processing after interrupt window closes.
     */
    public void continueAfterInterrupt(BonkersGame game, Player player) {
        BonkersPlayer bp = game.gamePlayer(player);
        BoardPosition position = bp.getPosition();

        // Step 5: Check for re-roll conditions
        boolean hasOtherPlayer = game.hasOtherPlayerOnSpace(player, position);
        ActionCard cardOnSpace = game.getCardOnSpace(position);
        ActionCardType fixedAction = position.fixedAction();

        boolean hasRollAgain = (cardOnSpace != null && cardOnSpace.type() == ActionCardType.ROLL_AGAIN)
                || (fixedAction == ActionCardType.ROLL_AGAIN);

        if (hasOtherPlayer || hasRollAgain) {
            // Go back to step 1 - roll again
            game.awaitRoll();
            return;
        }

        // Step 6: Check for card placement (only on blank spaces)
        if (position.isBlank()
                && !game.hasPlacedCardThisTurn()
                && bp.hasActionCards()
                && cardOnSpace == null) {
            game.awaitCardPlacement();
            return;
        }

        // After card placement or if no placement needed, check for actions
        checkAndExecuteAction(game, player);
    }

    /**
     * Check for and execute action on current space.
     */
    public void checkAndExecuteAction(BonkersGame game, Player player) {
        BonkersPlayer bp = game.gamePlayer(player);
        BoardPosition position = bp.getPosition();

        // Check for action on space
        ActionCard cardOnSpace = game.getCardOnSpace(position);
        ActionCardType fixedAction = position.fixedAction();
        ActionCardType actionType = null;

        if (cardOnSpace != null) {
            actionType = cardOnSpace.type();
        } else if (fixedAction != null) {
            actionType = fixedAction;
        }

        if (actionType != null) {
            if (!game.recordActionRun(position)) {
                // The actions have sent the piece round in a circle. The rules don't cover this, so the
                // turn ends with the piece where it is rather than looping forever.
                endTurn(game);
                return;
            }
            executeAction(game, player, actionType, position);
            return;
        }

        // Step 8: No action, turn ends
        endTurn(game);
    }

    /**
     * Execute an action card effect.
     */
    private void executeAction(
            BonkersGame game, Player player, ActionCardType actionType, BoardPosition sourcePosition) {
        BonkersPlayer bp = game.gamePlayer(player);

        switch (actionType) {
            case GO_TO_LOSE -> {
                bp.setPosition(BoardPosition.LOSE);
                processLanding(game, player);
            }
            case GO_TO_START -> {
                bp.setPosition(BoardPosition.START);
                processLanding(game, player);
            }
            case GO_TO_NEAREST_SCORE -> {
                var nearest = bp.getPosition().nearestScores();
                if (!nearest.isTie()) {
                    bp.setPosition(nearest.nearest());
                    processLanding(game, player);
                } else {
                    // Tie - player must choose direction
                    game.awaitDirection(nearest);
                }
            }
            case EXCHANGE_CARDS -> {
                // Player must choose which card to swap
                if (game.getSpacesWithActionCards().isEmpty() ||
                        (game.getSpacesWithActionCards().size() == 1 && game.getSpacesWithActionCards().get(0).equals(sourcePosition))) {
                    // No other cards to exchange with, skip this action
                    endTurn(game);
                } else {
                    game.awaitExchange(sourcePosition);
                }
            }
            case ROLL_AGAIN -> {
                // This should have been handled in step 5, but handle it here for safety
                game.awaitRoll();
            }
            default -> {
                // Movement actions (AHEAD_n, BACK_n, BACK_15)
                if (actionType.isMovementAction()) {
                    bp.setPosition(bp.getPosition().plus(actionType.getMovement()));
                    processLanding(game, player);
                } else {
                    // Unknown action, end turn
                    endTurn(game);
                }
            }
        }
    }

    /**
     * End the current turn and move to next player.
     */
    public void endTurn(BonkersGame game) {
        game.nextPlayer();
        game.startNewTurn();
    }
}
