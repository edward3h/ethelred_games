package org.ethelred.games.bonkers;

import org.ethelred.games.core.ActionDefinition;
import org.ethelred.games.core.BaseGame;
import org.ethelred.games.core.Player;
import org.ethelred.games.core.PlayerView;
import org.ethelred.games.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class BonkersGame extends BaseGame<BonkersPlayer> {
    private final Random random;
    private ActionDeck deck;

    // Board state: position -> card placed there
    private final Map<BoardPosition, ActionCard> boardCards = new HashMap<>();

    // Turn state
    private TurnPhase turnPhase = TurnPhase.WAITING_FOR_ROLL;
    private int lastDiceRoll;
    private boolean hasPlacedCardThisTurn;

    // For GoToNearestScore when there's a tie
    private BoardPosition.NearestScores pendingScores;

    // Positions whose action has run since the last roll or board change, in order, to spot action cards looping
    private final List<BoardPosition> actionChain = new ArrayList<>();

    // For Exchange Cards
    private BoardPosition exchangeCardSource;

    public BonkersGame(long id) {
        this(id, new Random());
    }

    public BonkersGame(long id, Random random) {
        super(id);
        this.random = random;
    }

    @Override
    public String type() {
        return BonkersGameDefinition.BONKERS;
    }

    @Override
    protected void start() {
        deck = new ActionDeck(random);

        // Deal 4 action cards to each player
        for (int i = 0; i < 4; i++) {
            eachPlayer((p, gp) -> {
                ActionCard card = deck.draw();
                if (card != null) {
                    gp.giveCard(card);
                }
            });
        }

        // All players start at position 0 with score 0 and 1 Lose card
        // (BonkersPlayer defaults handle this)

        turnPhase = TurnPhase.WAITING_FOR_ROLL;
    }

    @Override
    protected int nextPlayerIncrement() {
        return 1;
    }

    @Override
    protected BonkersPlayer createGamePlayer() {
        return new BonkersPlayer();
    }

    @Override
    protected int maxPlayers() {
        return 4;
    }

    @Override
    protected int minPlayers() {
        return 2;
    }

    @Override
    public PlayerView playerView(Player player) {
        return new BonkersPlayerView(player, this);
    }

    // State queries - package-private, used by the performers and the player view

    TurnPhase getTurnPhase() {
        return turnPhase;
    }

    int getLastDiceRoll() {
        return lastDiceRoll;
    }

    boolean hasPlacedCardThisTurn() {
        return hasPlacedCardThisTurn;
    }

    /** The tied Score spaces the active player must choose between, or null. */
    BoardPosition.NearestScores getPendingScores() {
        return pendingScores;
    }

    /** Where the Exchange Cards card being resolved sits, or null. */
    BoardPosition getExchangeCardSource() {
        return exchangeCardSource;
    }

    Map<BoardPosition, ActionCard> getBoardCards() {
        return boardCards;
    }

    ActionCard getCardOnSpace(BoardPosition position) {
        return boardCards.get(position);
    }

    ActionDeck getDeck() {
        return deck;
    }

    // Turn phase transitions - each one sets the phase together with the state that goes with it

    void awaitRoll() {
        turnPhase = TurnPhase.WAITING_FOR_ROLL;
    }

    void awaitCardPlacement() {
        turnPhase = TurnPhase.WAITING_FOR_CARD_PLACEMENT;
    }

    void awaitDirection(BoardPosition.NearestScores tiedScores) {
        pendingScores = tiedScores;
        turnPhase = TurnPhase.WAITING_FOR_DIRECTION;
    }

    void clearPendingScores() {
        pendingScores = null;
    }

    void awaitExchange(BoardPosition source) {
        exchangeCardSource = source;
        turnPhase = TurnPhase.WAITING_FOR_EXCHANGE_CHOICE;
    }

    // Changes to the board

    void placeCardOnSpace(BoardPosition position, ActionCard card) {
        boardCards.put(position, card);
        actionChain.clear();
    }

    /** The active player places a card from their hand, which uses up their placement for this turn. */
    void placeCardFromHand(BoardPosition position, ActionCard card) {
        placeCardOnSpace(position, card);
        hasPlacedCardThisTurn = true;
    }

    /** Swap the Exchange Cards card with the card on the target space, and finish the exchange. */
    void completeExchange(BoardPosition target) {
        ActionCard exchange = boardCards.get(exchangeCardSource);
        boardCards.put(exchangeCardSource, boardCards.get(target));
        boardCards.put(target, exchange);
        exchangeCardSource = null;
        actionChain.clear();
    }

    // Dice rolling
    int rollDice() {
        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        lastDiceRoll = die1 + die2;
        actionChain.clear();
        return lastDiceRoll;
    }

    // Check if another player is on a position
    boolean hasOtherPlayerOnSpace(Player currentPlayer, BoardPosition position) {
        boolean[] found = {false};
        eachPlayer((p, gp) -> {
            if (!p.same(currentPlayer) && gp.getPosition().equals(position)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    // Get positions that have action cards (for Exchange Cards)
    List<BoardPosition> getSpacesWithActionCards() {
        return boardCards.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Record that the action on a space is about to run. Returns null normally. If the action has already run in
     * this chain with nothing changed since, the actions are sending the piece round in a circle, and this returns
     * the spaces in that cycle in the order they were visited.
     */
    List<BoardPosition> recordActionRun(BoardPosition position) {
        int first = actionChain.indexOf(position);
        if (first < 0) {
            actionChain.add(position);
            return null;
        }
        return List.copyOf(actionChain.subList(first, actionChain.size()));
    }

    /** Whether the player holds a Lose card they can play on the active player right now. */
    boolean canPlayLoseCard(Player player) {
        return status() == Status.IN_PROGRESS && !player.same(currentPlayer()) && gamePlayer(player).hasLoseCard();
    }

    // Reset turn state for new turn
    void startNewTurn() {
        actionChain.clear();
        hasPlacedCardThisTurn = false;
        pendingScores = null;
        exchangeCardSource = null;
        turnPhase = TurnPhase.WAITING_FOR_ROLL;
    }

    // Actions for players based on current game state
    @Override
    public Set<ActionDefinition> actionsFor(Player player) {
        if (status() != Status.IN_PROGRESS) {
            return super.actionsFor(player);
        }

        Set<ActionDefinition> actions = new TreeSet<>(super.actionsFor(player));

        // A Lose card can be played on the active player at any time during their turn
        if (canPlayLoseCard(player)) {
            actions.add(new ActionDefinition("playLoseCard"));
        }

        // Only current player can take turn actions
        if (!player.same(currentPlayer())) {
            return Set.copyOf(actions);
        }

        BonkersPlayer bp = gamePlayer(player);

        switch (turnPhase) {
            case WAITING_FOR_ROLL -> {
                actions.add(new ActionDefinition("rollDice"));
            }
            case WAITING_FOR_CARD_PLACEMENT -> {
                Set<String> cardCodes = bp.getActionCards().stream()
                        .map(ActionCard::shortCode)
                        .collect(Collectors.toSet());
                if (!cardCodes.isEmpty()) {
                    actions.add(new ActionDefinition("placeCard", cardCodes));
                }
            }
            case WAITING_FOR_EXCHANGE_CHOICE -> {
                // Exchanging is optional, so the player can always decline
                Set<String> choices = getSpacesWithActionCards().stream()
                        .filter(pos -> !pos.equals(exchangeCardSource))
                        .map(String::valueOf)
                        .collect(Collectors.toCollection(HashSet::new));
                choices.add(ChooseExchangeCardPerformer.DECLINE);
                actions.add(new ActionDefinition("chooseExchangeCard", choices));
            }
            case WAITING_FOR_DIRECTION -> {
                if (pendingScores != null) {
                    actions.add(new ActionDefinition("chooseDirection", "forward", "backward"));
                }
            }
            default -> {
                // No actions during other phases
            }
        }

        return Set.copyOf(actions);
    }
}
