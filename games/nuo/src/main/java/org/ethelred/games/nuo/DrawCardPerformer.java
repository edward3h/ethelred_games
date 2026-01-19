package org.ethelred.games.nuo;

import org.ethelred.games.core.Action;
import org.ethelred.games.core.ActionPerformer;
import org.ethelred.games.core.Game;
import org.ethelred.games.core.Player;

public class DrawCardPerformer implements ActionPerformer<NuoGame>
{
    public static final String NAME = "drawCard";

    @Override
    public String actionName()
    {
        return NAME;
    }

    @Override
    public void perform(NuoGame game, Player player, Action action)
    {
        validate(game.status() == Game.Status.IN_PROGRESS, "Game is not in progress");
        validate(game.playState() == NuoGame.PlayState.NORMAL, "Not expected state");
        var currentPlayer = game.currentPlayer();
        validate(player.same(currentPlayer), "Not the current player");
        var card = game.takeCard();
        var np = game.gamePlayer(player);
        np.giveCard(card);
        if (PlayCardPerformer.isValidPlay(game, np, card))
        {
            game.playState(NuoGame.PlayState.PLAY_DRAWN);
        }
        else
        {
            if (!game.houseRules[1]) {
                game.nextPlayer();
            }
        }
    }
}
