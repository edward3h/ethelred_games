package org.ethelred.games.nuo

import org.ethelred.games.core.BasePlayer
import org.ethelred.games.core.StringAction
import spock.lang.Specification

class TestGamePlay extends Specification {
    def 'test valid card plays'(play, current, wildColor) {
        given:
        def game = setupGame(['r6', 'b7', 'g7', 'bs', 'rs', 'bd', 'xx'], current, ['y1', 'y2'], wildColor)
        def player = game.currentPlayer()
        def performer = new PlayCardPerformer()

        when:
        def action = new StringAction(PlayCardPerformer.NAME, play)
        performer.perform(game, player, action)

        then:
            game.current() == Card.fromCode(play)
            game.playState() == NuoGame.PlayState.NORMAL
        play == 'bd' ? game.currentPlayer() == player : game.currentPlayer() != player

        where:
        play | current | wildColor
        'r6' | 'b6' | null
        'b7' | 'b6' | null
        'bd' | 'b6' | null
        'r6' | 'xw' | Color.RED
        'r6' | 'xx' | Color.RED
    }

    def 'test invalid card plays'(play, current, wildColor) {
        given:
        def game = setupGame(['r6', 'b7', 'g7', 'bs', 'rs', 'bd', 'xx'], current, ['y1', 'y2'], wildColor)
        def player = game.currentPlayer()
        def performer = new PlayCardPerformer()

        when:
        def action = new StringAction(PlayCardPerformer.NAME, play)
        performer.perform(game, player, action)

        then:
        thrown(RuntimeException)
        game.currentPlayer() == player

        where:
        play | current | wildColor
        'b3' | 'b6' | null
        'rs' | 'b6' | null
        'zy' | 'b6' | null
//        'xx' | 'b6' | null
        'r6' | 'xw' | Color.YELLOW
        'r6' | 'xx' | Color.YELLOW
    }

    def 'test reverse order'() {
        given:
        def performer = new PlayCardPerformer()
        def game = new NuoGame(1, new MockDeck(['rr'], 'r1', ['y1'], 3))
        def players = (0..2).collect { new BasePlayer(it)}
        players.each { game.addPlayer(it)}
        players.each { game.playerReady(it)}

        when:
        def action = new StringAction(PlayCardPerformer.NAME, 'rr')
        performer.perform(game, players[0], action)

        then:
        game.currentPlayer().same(players[2])

        when:
        game.nextPlayer()

        then:
        game.currentPlayer().same(players[1])
    }

    def setupGame(hand, current, more = [], Color wildColor = null) {
        def game = new NuoGame(1, new MockDeck(hand, current, more))
        def player1 = new BasePlayer(1)
        game.addPlayer(player1)
        def player2 = new BasePlayer(2)
        game.addPlayer(player2)
        game.playerReady(player1)
        game.playerReady(player2)
        game.wildColor(wildColor)
        game.current(Card.fromCode(current))
        game
    }
}
