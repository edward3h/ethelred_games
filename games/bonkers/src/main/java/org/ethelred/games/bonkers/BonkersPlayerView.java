package org.ethelred.games.bonkers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethelred.games.core.BasePlayerView;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.Player;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BonkersPlayerView extends BasePlayerView<BonkersPlayer> {

    @JsonProperty
    private TurnPhase turnPhase;

    @JsonProperty
    private int lastDiceRoll;

    @JsonProperty
    private Map<Integer, ActionCard> boardCards;

    @JsonProperty
    private List<ActionCard> myActionCards;

    @JsonProperty
    private boolean myLoseCard;

    @JsonProperty
    private int myPosition;

    @JsonProperty
    private int myScore;

    @JsonProperty
    private int[] nearestScoreOptions;

    @JsonProperty
    private boolean canInterrupt;

    public BonkersPlayerView(Player player, BonkersGame game) {
        super(player, game);
        if (game.status() == Game.Status.PRESTART) {
            return;
        }

        turnPhase = game.getTurnPhase();
        lastDiceRoll = game.getLastDiceRoll();
        boardCards = Map.copyOf(game.getBoardCards());

        BonkersPlayer bp = game.gamePlayer(player);
        myActionCards = List.copyOf(bp.getActionCards());
        myLoseCard = bp.hasLoseCard();
        myPosition = bp.getPosition();
        myScore = bp.getScore();

        // Only show direction options when in that phase
        if (game.getTurnPhase() == TurnPhase.WAITING_FOR_DIRECTION) {
            nearestScoreOptions = game.getPendingScoreOptions();
        }

        // Show if this player can use their Lose card to interrupt
        canInterrupt = game.canPlayerInterrupt(player);
    }
}
