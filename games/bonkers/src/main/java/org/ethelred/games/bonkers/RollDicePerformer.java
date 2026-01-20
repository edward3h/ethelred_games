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
        int oldPosition = bp.getPosition();
        int newPosition = Board.move(oldPosition, roll);
        bp.setPosition(newPosition);

        // Process the landing
        processLanding(game, player);
    }

    /**
     * Process landing on a space. This handles steps 3-8 of the turn flow.
     * Can be called recursively when actions cause movement.
     */
    public void processLanding(BonkersGame game, Player player) {
        BonkersPlayer bp = game.gamePlayer(player);
        int position = bp.getPosition();

        // Step 3: Check for Score space
        if (Board.isScoreSpace(position)) {
            bp.addScore(1);
            if (bp.hasWon()) {
                game.winner(player);
                return;
            }
        }

        // Step 4: Check for Lose space
        if (Board.isLoseSpace(position)) {
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
        int position = bp.getPosition();

        // Step 5: Check for re-roll conditions
        boolean hasOtherPlayer = game.hasOtherPlayerOnSpace(player, position);
        ActionCard cardOnSpace = game.getCardOnSpace(position);
        ActionCardType fixedAction = Board.getFixedAction(position);

        boolean hasRollAgain = (cardOnSpace != null && cardOnSpace.type() == ActionCardType.ROLL_AGAIN)
                || (fixedAction == ActionCardType.ROLL_AGAIN);

        if (hasOtherPlayer || hasRollAgain) {
            // Go back to step 1 - roll again
            game.setTurnPhase(TurnPhase.WAITING_FOR_ROLL);
            return;
        }

        // Step 6: Check for card placement (only on blank spaces)
        if (Board.isBlankSpace(position)
                && !game.hasPlacedCardThisTurn()
                && bp.hasActionCards()
                && cardOnSpace == null) {
            game.setTurnPhase(TurnPhase.WAITING_FOR_CARD_PLACEMENT);
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
        int position = bp.getPosition();

        // Check for action on space
        ActionCard cardOnSpace = game.getCardOnSpace(position);
        ActionCardType fixedAction = Board.getFixedAction(position);
        ActionCardType actionType = null;

        if (cardOnSpace != null) {
            actionType = cardOnSpace.type();
        } else if (fixedAction != null) {
            actionType = fixedAction;
        }

        if (actionType != null) {
            executeAction(game, player, actionType, position);
            return;
        }

        // Step 8: No action, turn ends
        endTurn(game);
    }

    /**
     * Execute an action card effect.
     */
    private void executeAction(BonkersGame game, Player player, ActionCardType actionType, int sourcePosition) {
        BonkersPlayer bp = game.gamePlayer(player);

        switch (actionType) {
            case GO_TO_LOSE -> {
                bp.setPosition(Board.LOSE_POSITION);
                processLanding(game, player);
            }
            case GO_TO_START -> {
                bp.setPosition(Board.START_POSITION);
                processLanding(game, player);
            }
            case GO_TO_NEAREST_SCORE -> {
                int[] options = Board.findNearestScorePositions(bp.getPosition());
                if (options.length == 1) {
                    bp.setPosition(options[0]);
                    processLanding(game, player);
                } else {
                    // Tie - player must choose direction
                    game.setPendingScoreOptions(options);
                    game.setTurnPhase(TurnPhase.WAITING_FOR_DIRECTION);
                }
            }
            case EXCHANGE_CARDS -> {
                // Player must choose which card to swap
                if (game.getSpacesWithActionCards().isEmpty() ||
                        (game.getSpacesWithActionCards().size() == 1 && game.getSpacesWithActionCards().get(0) == sourcePosition)) {
                    // No other cards to exchange with, skip this action
                    endTurn(game);
                } else {
                    game.setExchangeCardSourcePosition(sourcePosition);
                    game.setTurnPhase(TurnPhase.WAITING_FOR_EXCHANGE_CHOICE);
                }
            }
            case ROLL_AGAIN -> {
                // This should have been handled in step 5, but handle it here for safety
                game.setTurnPhase(TurnPhase.WAITING_FOR_ROLL);
            }
            default -> {
                // Movement actions (AHEAD_n, BACK_n, BACK_15)
                if (actionType.isMovementAction()) {
                    int newPosition = Board.move(bp.getPosition(), actionType.getMovement());
                    bp.setPosition(newPosition);
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
