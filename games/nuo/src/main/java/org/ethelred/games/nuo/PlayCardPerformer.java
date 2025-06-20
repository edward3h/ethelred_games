package org.ethelred.games.nuo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.InvalidActionException;
import org.ethelred.games.core.Player;

public class PlayCardPerformer implements ActionPerformer<NuoGame>
{
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String NAME = "playCard";

    @Override
    public String actionName()
    {
        return NAME;
    }

    @Override
    public void perform(NuoGame game, Player player, Action action)
    {
        playCard(game, player, action.argumentAsString());
    }

    /* package */ void playCard(NuoGame game, Player player, String cardCode)
    {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.playState() == NuoGame.PlayState.NORMAL, "Not the expected state");

        // check that the player sent the action is actually the player whose turn it is
        var currentPlayer = game.currentPlayer();
        validate(currentPlayer.same(player), "Not the current player");

        // check that code is a valid card
        Card card;
        try
        {
            card = Card.fromCode(cardCode);
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidActionException(e);
        }

        var np = game.gamePlayer(player);

        // check if it is a match for the current card
        if (!isValidPlay(game, np, card))
        {
            throw new InvalidActionException("Invalid play " + card.shortCode());
        }

        // check that player has the card
        card = np.takeCard(card);
        if (card == null)
        {
            throw new InvalidActionException("Player did not have card " + cardCode);
        }

        // if we get here we think the player has the card and it's a valid one to play, so go ahead
        game.current(card);
        game.wildColor(null); // any previously chosen color is not relevant

        if (np.hand().isEmpty())
        {
            game.winner(currentPlayer);
            return;
        }

        // resolve effect if any
        switch (card.type())
        {
            case WILD -> game.playState(NuoGame.PlayState.CHOOSE_COLOR);
            case DRAW_FOUR -> {
                var nextPlayer = game.gamePlayer(game.peekNextPlayer());
                for (int i = 0; i < 4; i++)
                {
                    nextPlayer.giveCard(game.takeCard());
                }
                game.playState(NuoGame.PlayState.CHOOSE_COLOR);
            }
            case NUMBER -> game.nextPlayer();
            case DRAW_TWO -> {
                var nextPlayer = game.gamePlayer(game.nextPlayer());
                for (int i = 0; i < 2; i++)
                {
                    nextPlayer.giveCard(game.takeCard());
                }
                game.nextPlayer();
            }
            case REVERSE -> {
                if (game.playerCount() > 2)
                {
                    game.reverse();
                    game.nextPlayer();
                }
                // in two player games, reverse acts as skip, which brings play back to the same player so nothing happens
            }
            case SKIP -> {
                game.nextPlayer();
                game.nextPlayer();
            }
        }
    }

    public static boolean isValidPlay(NuoGame game, NuoPlayer np, Card card)
    {
        var current = game.current();
        LOGGER.debug("isValidPlay? current {} played {}", current, card);
        if (!game.houseRules[0] && card.color() == Color.WILD && card.type() == Card.Type.DRAW_FOUR)
        {
            // check that the player has no other playable cards in hand
            return np.hand().stream().filter(c -> c.type() != Card.Type.DRAW_FOUR).noneMatch(c -> isValidPlay(game, np, c));
        }
        else if (card.color() == Color.WILD)
        {
            return true;
        }
        else
        {
            Color wildColor = game.wildColor();
            if (card.code() == current.code())
            {
                return true;
            } else if (card.color() == current.color())
            {
                return true;
            }
            else {
                return current.type().isWild() && card.color() == wildColor;
            }
        }
    }

}
