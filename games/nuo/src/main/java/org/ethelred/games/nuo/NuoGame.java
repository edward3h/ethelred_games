package org.ethelred.games.nuo;

import org.ethelred.games.core.ActionDefinition;
import org.ethelred.games.core.BaseGame;
import org.ethelred.games.core.Player;
import org.ethelred.games.core.PlayerView;
import org.ethelred.games.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NuoGame extends BaseGame<NuoPlayer>
{
    enum PlayState
    {
        NORMAL,
        CHOOSE_COLOR,
        PLAY_DRAWN
    }

    boolean[] houseRules = new boolean[2];
    private final Deck deck;
    boolean reversedDirection;
    private @Nullable
    Card current;
    private Color wildColor;
    @NotNull
    PlayState playState = PlayState.NORMAL;
    // TODO last card + challenge states

    public NuoGame(long id, Deck deck)
    {
        super(id);
        this.deck = deck;
        houseRules[0] = true;
        houseRules[1] = true;
    }

    @Override
    public String type()
    {
        return NuoGameDefinition.NUO;
    }

    protected void start()
    {
        IntStream.rangeClosed(1, 7).forEach(n -> eachPlayer((p, gp) -> gp.giveCard(deck.takeCard())));
        current = deck.takeCard();
        while (current.color() == Color.WILD) {
            deck.discard(current);
            current = deck.takeCard();
        }
    }

    @Override
    protected int nextPlayerIncrement()
    {
        return reversedDirection ? -1 : 1;
    }

    Card current()
    {
        return Objects.requireNonNull(current);
    }

    public void current(Card card)
    {
        deck.discard(Objects.requireNonNull(current));
        current = card;
    }

    public void wildColor(Color color)
    {
        wildColor = color;
    }

    void reverse()
    {
        reversedDirection = !reversedDirection;
    }

    PlayState playState()
    {
        return playState;
    }

    void playState(PlayState playState)
    {
        this.playState = playState;
    }

    Color wildColor()
    {
        return wildColor;
    }

    @Override
    protected NuoPlayer createGamePlayer()
    {
        return new NuoPlayer();
    }

    @Override
    protected int maxPlayers()
    {
        return 10;
    }

    @Override
    protected int minPlayers()
    {
        return 2;
    }


    @Override
    public PlayerView playerView(Player player)
    {
        return new NuoPlayerView(player, this);
    }

    Card takeCard()
    {
        return deck.takeCard();
    }

    @Override
    public Set<ActionDefinition> actionsFor(Player player) {
        if (status() == Status.IN_PROGRESS && player.same(currentPlayer())) {
            return Util.merge(getPlayerTurnActions(gamePlayer(player)), super.actionsFor(player));
        }
        return super.actionsFor(player);
    }

    private Set<ActionDefinition> getPlayerTurnActions(NuoPlayer nuoPlayer)
    {
        Set<ActionDefinition> result = new TreeSet<>();
        switch (playState)
        {
            case NORMAL -> {
                var validCards = nuoPlayer.hand()
                        .elementSet()
                        .stream()
                        .filter(card -> PlayCardPerformer.isValidPlay(this, nuoPlayer, card))
                        .map(Card::shortCode)
                                .collect(Collectors.toSet());
                if (!validCards.isEmpty()) {
                    result.add(new ActionDefinition(PlayCardPerformer.NAME, validCards));
                }
                result.add(new ActionDefinition(DrawCardPerformer.NAME));
            }
            case CHOOSE_COLOR -> result.add(new ActionDefinition(ChooseColorPerformer.NAME, Color.RED.shortCode(), Color.GREEN.shortCode(), Color.BLUE.shortCode(), Color.YELLOW.shortCode()));
            case PLAY_DRAWN -> result.add(new ActionDefinition(PlayDrawnPerformer.NAME, "true", "false"));
        }
        return result;
    }

}
