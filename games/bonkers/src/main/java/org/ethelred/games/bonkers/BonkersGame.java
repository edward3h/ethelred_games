package org.ethelred.games.bonkers;

import org.ethelred.games.core.ActionDefinition;
import org.ethelred.games.core.BaseGame;
import org.ethelred.games.core.Player;
import org.ethelred.games.core.PlayerView;
import org.ethelred.games.util.Util;

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

    // For Exchange Cards
    private BoardPosition exchangeCardSource;

    // Lose card interrupt state
    private Set<Player> playersWhoCanInterrupt = new HashSet<>();
    private Set<Player> playersWhoPassed = new HashSet<>();

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

    // Getters for state
    public TurnPhase getTurnPhase() {
        return turnPhase;
    }

    public void setTurnPhase(TurnPhase turnPhase) {
        this.turnPhase = turnPhase;
    }

    public int getLastDiceRoll() {
        return lastDiceRoll;
    }

    public void setLastDiceRoll(int lastDiceRoll) {
        this.lastDiceRoll = lastDiceRoll;
    }

    public boolean hasPlacedCardThisTurn() {
        return hasPlacedCardThisTurn;
    }

    public void setHasPlacedCardThisTurn(boolean hasPlacedCardThisTurn) {
        this.hasPlacedCardThisTurn = hasPlacedCardThisTurn;
    }

    /** The tied Score spaces the active player must choose between, or null. */
    public BoardPosition.NearestScores getPendingScores() {
        return pendingScores;
    }

    public void setPendingScores(BoardPosition.NearestScores pendingScores) {
        this.pendingScores = pendingScores;
    }

    /** Where the Exchange Cards card being resolved sits, or null. */
    public BoardPosition getExchangeCardSource() {
        return exchangeCardSource;
    }

    public void setExchangeCardSource(BoardPosition position) {
        this.exchangeCardSource = position;
    }

    public Map<BoardPosition, ActionCard> getBoardCards() {
        return boardCards;
    }

    public ActionCard getCardOnSpace(BoardPosition position) {
        return boardCards.get(position);
    }

    public void placeCardOnSpace(BoardPosition position, ActionCard card) {
        boardCards.put(position, card);
    }

    public ActionCard removeCardFromSpace(BoardPosition position) {
        return boardCards.remove(position);
    }

    public ActionDeck getDeck() {
        return deck;
    }

    public Random getRandom() {
        return random;
    }

    // Dice rolling
    public int rollDice() {
        int die1 = random.nextInt(6) + 1;
        int die2 = random.nextInt(6) + 1;
        lastDiceRoll = die1 + die2;
        return lastDiceRoll;
    }

    // Check if another player is on a position
    public boolean hasOtherPlayerOnSpace(Player currentPlayer, BoardPosition position) {
        boolean[] found = {false};
        eachPlayer((p, gp) -> {
            if (!p.same(currentPlayer) && gp.getPosition().equals(position)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    // Get positions that have action cards (for Exchange Cards)
    public List<BoardPosition> getSpacesWithActionCards() {
        return boardCards.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    // Lose card interrupt handling
    public void openLoseInterruptWindow() {
        playersWhoCanInterrupt = new HashSet<>();
        playersWhoPassed = new HashSet<>();

        eachPlayer((p, gp) -> {
            if (!p.same(currentPlayer()) && gp.hasLoseCard()) {
                playersWhoCanInterrupt.add(p);
            }
        });

        if (!playersWhoCanInterrupt.isEmpty()) {
            turnPhase = TurnPhase.WAITING_FOR_LOSE_INTERRUPT;
        }
    }

    public void playerPassedInterrupt(Player player) {
        playersWhoPassed.add(player);
    }

    public boolean allPlayersRespondedToInterrupt() {
        return playersWhoPassed.containsAll(playersWhoCanInterrupt);
    }

    public boolean canPlayerInterrupt(Player player) {
        return playersWhoCanInterrupt.contains(player) && !playersWhoPassed.contains(player);
    }

    public Set<Player> getPlayersWhoCanInterrupt() {
        return playersWhoCanInterrupt;
    }

    public void clearInterruptWindow() {
        playersWhoCanInterrupt.clear();
        playersWhoPassed.clear();
    }

    // Reset turn state for new turn
    public void startNewTurn() {
        hasPlacedCardThisTurn = false;
        pendingScores = null;
        exchangeCardSource = null;
        clearInterruptWindow();
        turnPhase = TurnPhase.WAITING_FOR_ROLL;
    }

    // Actions for players based on current game state
    @Override
    public Set<ActionDefinition> actionsFor(Player player) {
        if (status() != Status.IN_PROGRESS) {
            return super.actionsFor(player);
        }

        Set<ActionDefinition> actions = new TreeSet<>(super.actionsFor(player));

        // Lose card interrupt - available to non-current players with Lose cards during interrupt window
        if (turnPhase == TurnPhase.WAITING_FOR_LOSE_INTERRUPT) {
            if (canPlayerInterrupt(player)) {
                actions.add(new ActionDefinition("playLoseCard"));
                actions.add(new ActionDefinition("passLoseInterrupt"));
            }
            return Set.copyOf(actions);
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
                Set<String> positions = getSpacesWithActionCards().stream()
                        .filter(pos -> !pos.equals(exchangeCardSource))
                        .map(String::valueOf)
                        .collect(Collectors.toSet());
                if (!positions.isEmpty()) {
                    actions.add(new ActionDefinition("chooseExchangeCard", positions));
                }
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
