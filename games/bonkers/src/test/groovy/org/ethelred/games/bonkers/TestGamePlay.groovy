package org.ethelred.games.bonkers

import com.fasterxml.jackson.databind.ObjectMapper
import org.ethelred.games.core.ActionDefinition
import org.ethelred.games.core.BasePlayer
import org.ethelred.games.core.Game
import org.ethelred.games.core.InvalidActionException
import org.ethelred.games.core.StringAction
import spock.lang.Specification

class TestGamePlay extends Specification {
    ScriptedRandom dice = new ScriptedRandom()
    RollDicePerformer rollPerformer = new RollDicePerformer()
    PlaceCardPerformer placePerformer = new PlaceCardPerformer(rollPerformer)
    ChooseExchangeCardPerformer exchangePerformer = new ChooseExchangeCardPerformer(rollPerformer)
    ChooseDirectionPerformer directionPerformer = new ChooseDirectionPerformer(rollPerformer)
    PlayLoseCardPerformer playLosePerformer = new PlayLoseCardPerformer(rollPerformer)

    // ---- setup -----------------------------------------------------------------------------

    def 'game starts when players are ready with the setup from the rules'() {
        given:
        def game = new BonkersGame(1, dice)
        def p1 = new BasePlayer(1)
        def p2 = new BasePlayer(2)
        game.addPlayer(p1)
        game.addPlayer(p2)

        when:
        game.playerReady(p1)

        then:
        game.status() == Game.Status.PRESTART

        when:
        game.playerReady(p2)

        then:
        game.status() == Game.Status.IN_PROGRESS
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
        [p1, p2].every {
            def bp = game.gamePlayer(it)
            bp.actionCards.size() == 4 && bp.hasLoseCard() && bp.position == BoardPosition.of(0) && bp.score == 0
        }
        game.deck.remainingCards() == 40 - 8
    }

    def 'only the current player can roll'() {
        given:
        def (game, p1, p2) = setupGame()

        expect:
        game.actionsFor(p1)*.name().contains('rollDice')
        !game.actionsFor(p2)*.name().contains('rollDice')
    }

    // ---- rolling ---------------------------------------------------------------------------

    def 'rolling moves the piece by the dice total and offers card placement'() {
        given:
        def (game, p1, p2) = setupGame()

        when:
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(7)
        game.lastDiceRoll == 7
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT
        game.currentPlayer().same(p1)
    }

    def 'cannot roll out of turn or out of phase'() {
        given:
        def (game, p1, p2) = setupGame()

        when:
        roll(game, p2, 3, 4)

        then:
        thrown(InvalidActionException)

        when:
        roll(game, p1, 3, 4)
        roll(game, p1, 3, 4)

        then:
        thrown(InvalidActionException)
    }

    def 'rolling a 12 scores a point'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(1)

        when:
        roll(game, p1, 6, 6)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(13)
        game.gamePlayer(p1).score == 1
    }

    def 'rolling a 12 onto a score space scores twice'() {
        given:
        def (game, p1, p2) = setupGame()

        when:
        roll(game, p1, 6, 6)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(12)
        game.gamePlayer(p1).score == 2
    }

    def 'landing on a score space scores a point and ends the turn'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(5)

        when:
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(12)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'landing on the lose space loses a point and ends the turn'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(25)
        game.gamePlayer(p1).addScore(3)

        when:
        roll(game, p1, 3, 3)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.gamePlayer(p1).score == 2
        game.currentPlayer().same(p2)
    }

    def 'score does not go below zero'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(25)

        when:
        roll(game, p1, 3, 3)

        then:
        game.gamePlayer(p1).score == 0
    }

    def 'landing on another player means roll again'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p2).position = BoardPosition.of(7)

        when:
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(7)
        game.currentPlayer().same(p1)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'returning to Start while another player is there means roll again'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('goToStart', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, placed)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(0)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p1)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'landing on a roll again card means roll again'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(7), card('rollAgain', 100))

        when:
        roll(game, p1, 3, 4)

        then:
        game.currentPlayer().same(p1)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'blank space with no action cards in hand ends the turn'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).actionCards.clear()

        when:
        roll(game, p1, 3, 4)

        then:
        game.currentPlayer().same(p2)
    }

    def 'fixed Go To Lose space sends the player to Lose'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(30)
        game.gamePlayer(p1).addScore(3)

        when:
        roll(game, p1, 2, 3)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.gamePlayer(p1).score == 2
        game.currentPlayer().same(p2)
    }

    def 'fixed Back 15 space moves the player back 15'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).position = BoardPosition.of(40)

        when:
        roll(game, p1, 1, 2)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(28)
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT
    }

    def 'landing on an action card runs it without placing another'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(7), card('ahead3', 100))

        when:
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(10)
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT
    }

    def 'entrapment: Back then Ahead cards score a point and leave the piece on the forward space'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(23), card('back3', 100))
        game.placeCardOnSpace(BoardPosition.of(20), card('ahead3', 101))
        game.gamePlayer(p1).position = BoardPosition.of(16)

        when: 'landing on the Back 3 first'
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(20)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'entrapment: landing on the Ahead card first gives the same result'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(20), card('ahead3', 100))
        game.placeCardOnSpace(BoardPosition.of(23), card('back3', 101))
        game.gamePlayer(p1).position = BoardPosition.of(13)

        when:
        roll(game, p1, 3, 4)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(20)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
    }

    def 'entrapment through the fixed Back 15 space'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(28), card('ahead10', 100))
        game.placeCardOnSpace(BoardPosition.of(38), card('ahead5', 101))
        game.gamePlayer(p1).position = BoardPosition.of(21)

        when: '28 -> 38 -> 43 (Back 15) -> 28 again'
        roll(game, p1, 3, 4)

        then: 'the piece rests on the last forward space visited'
        game.gamePlayer(p1).position == BoardPosition.of(38)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
    }

    def 'entrapment formed by placing a card'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('back3', 100)
        game.placeCardOnSpace(BoardPosition.of(20), card('ahead3', 101))
        game.gamePlayer(p1).giveCard(placed)
        game.gamePlayer(p1).position = BoardPosition.of(16)
        roll(game, p1, 3, 4)

        expect: 'the piece lands on empty space 23 and is offered a placement'
        game.gamePlayer(p1).position == BoardPosition.of(23)
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT

        when:
        place(game, p1, placed)

        then: '23 (Back 3) -> 20 (Ahead 3) -> 23 again'
        game.gamePlayer(p1).position == BoardPosition.of(20)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
    }

    def 'entrapment can win the game'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(20), card('ahead3', 100))
        game.placeCardOnSpace(BoardPosition.of(23), card('back3', 101))
        game.gamePlayer(p1).position = BoardPosition.of(13)
        game.gamePlayer(p1).addScore(11)

        when:
        roll(game, p1, 3, 4)

        then:
        game.status() == Game.Status.ENDED
    }

    def 'a long chain of action cards that does not repeat still runs to the end'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(7), card('ahead3', 100))
        game.placeCardOnSpace(BoardPosition.of(10), card('ahead2', 101))
        game.gamePlayer(p1).actionCards.clear()

        when:
        roll(game, p1, 3, 4)

        then: '7 -> 10 -> 12, which scores and has no action'
        game.gamePlayer(p1).position == BoardPosition.of(12)
        game.gamePlayer(p1).score == 1
    }

    def 'turn order wraps round all players'() {
        given:
        def (game, p1, p2, p3) = setupGame(3)
        game.eachPlayer { p, gp -> gp.actionCards.clear() }

        when:
        roll(game, p1, 3, 4)

        then:
        game.currentPlayer().same(p2)

        when:
        roll(game, p2, 3, 5)

        then:
        game.currentPlayer().same(p3)

        when:
        roll(game, p3, 4, 5)

        then:
        game.currentPlayer().same(p1)
    }

    // ---- placing cards ---------------------------------------------------------------------

    def 'placing a card puts it on the space and draws a replacement'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('rollAgain', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, placed)

        then:
        game.getCardOnSpace(BoardPosition.of(7)) == placed
        !game.gamePlayer(p1).actionCards.contains(placed)
        game.gamePlayer(p1).actionCards.size() == 5
        game.deck.remainingCards() == 40 - 8 - 1
    }

    def 'placed card action runs, then the turn ends'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('ahead2', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, placed)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(9)
        game.getCardOnSpace(BoardPosition.of(7)) == placed
        game.currentPlayer().same(p2)
    }

    def 'placing Go To Start sends the player to Start where they score'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p2).position = BoardPosition.of(20)
        def placed = card('goToStart', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, placed)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(0)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
    }

    def 'placing Roll Again lets the player roll again'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('rollAgain', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, placed)

        then:
        game.currentPlayer().same(p1)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'only one card is placed per turn'() {
        given:
        def (game, p1, p2) = setupGame()
        def placed = card('rollAgain', 100)
        game.gamePlayer(p1).giveCard(placed)
        roll(game, p1, 3, 4)
        place(game, p1, placed)

        when: 'rolling again onto another blank space'
        roll(game, p1, 1, 2)

        then: 'no second placement is offered; the turn ends'
        game.gamePlayer(p1).position == BoardPosition.of(10)
        game.currentPlayer().same(p2)
    }

    def 'cannot place a card you do not hold or out of phase'() {
        given:
        def (game, p1, p2) = setupGame()

        when: 'placing before rolling'
        place(game, p1, game.gamePlayer(p1).actionCards[0])

        then:
        thrown(InvalidActionException)

        when: 'placing a card that is not in hand'
        roll(game, p1, 3, 4)
        place(game, p1, card('ahead1', 999))

        then:
        thrown(InvalidActionException)
    }

    def 'only the current player can place a card'() {
        given:
        def (game, p1, p2) = setupGame()
        roll(game, p1, 3, 4)

        when:
        place(game, p2, game.gamePlayer(p2).actionCards[0])

        then:
        thrown(InvalidActionException)
    }

    // ---- exchange cards --------------------------------------------------------------------

    def 'exchange cards swaps with another board card'() {
        given:
        def (game, p1, p2) = setupGame()
        def other = card('back3', 101)
        def exchange = card('exchangeCards', 100)
        game.placeCardOnSpace(BoardPosition.of(20), other)
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, exchange)

        then:
        game.turnPhase == TurnPhase.WAITING_FOR_EXCHANGE_CHOICE
        game.actionsFor(p1).contains(new ActionDefinition('chooseExchangeCard', '20', 'none'))

        when:
        exchangePerformer.perform(game, p1, new StringAction('chooseExchangeCard', '20'))

        then: 'cards swapped, and the swapped-in Back 3 then moves the player from 7 to 4'
        game.getCardOnSpace(BoardPosition.of(7)) == other
        game.getCardOnSpace(BoardPosition.of(20)) == exchange
        game.gamePlayer(p1).position == BoardPosition.of(4)
        game.currentPlayer().same(p2)
    }

    def 'exchange cards with nothing to swap with ends the turn'() {
        given:
        def (game, p1, p2) = setupGame()
        def exchange = card('exchangeCards', 100)
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)

        when:
        place(game, p1, exchange)

        then:
        game.getCardOnSpace(BoardPosition.of(7)) == exchange
        game.currentPlayer().same(p2)
    }

    def 'exchange target is normalised so a space cannot be exchanged with itself'() {
        given:
        def (game, p1, p2) = setupGame()
        def exchange = card('exchangeCards', 100)
        game.placeCardOnSpace(BoardPosition.of(20), card('back3', 101))
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)
        place(game, p1, exchange)

        when: 'space 62 is the same space as 7'
        exchangePerformer.perform(game, p1, new StringAction('chooseExchangeCard', '62'))

        then:
        thrown(InvalidActionException)
        game.getCardOnSpace(BoardPosition.of(7)) == exchange
    }

    def 'exchange cards is optional and can be declined'() {
        given:
        def (game, p1, p2) = setupGame()
        def exchange = card('exchangeCards', 100)
        def other = card('back3', 101)
        game.placeCardOnSpace(BoardPosition.of(20), other)
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)
        place(game, p1, exchange)

        expect:
        game.actionsFor(p1).contains(new ActionDefinition('chooseExchangeCard', '20', 'none'))

        when:
        exchangePerformer.perform(game, p1, new StringAction('chooseExchangeCard', 'none'))

        then: 'nothing moves and the turn ends'
        game.getCardOnSpace(BoardPosition.of(7)) == exchange
        game.getCardOnSpace(BoardPosition.of(20)) == other
        game.gamePlayer(p1).position == BoardPosition.of(7)
        game.currentPlayer().same(p2)
    }

    def 'cannot exchange with an empty space'() {
        given:
        def (game, p1, p2) = setupGame()
        def exchange = card('exchangeCards', 100)
        game.placeCardOnSpace(BoardPosition.of(20), card('back3', 101))
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)
        place(game, p1, exchange)

        when:
        exchangePerformer.perform(game, p1, new StringAction('chooseExchangeCard', '21'))

        then:
        thrown(InvalidActionException)
    }

    // ---- go to nearest score ---------------------------------------------------------------

    def 'go to nearest score with a single nearest space moves there'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p2).position = BoardPosition.of(20)
        def nearest = card('goToNearestScore', 100)
        game.gamePlayer(p1).giveCard(nearest)
        roll(game, p1, 2, 3)

        when:
        place(game, p1, nearest)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(0)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)
    }

    def 'go to nearest score with a tie asks for a direction'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p2).position = BoardPosition.of(20)
        def nearest = card('goToNearestScore', 100)
        game.gamePlayer(p1).giveCard(nearest)
        roll(game, p1, 3, 3)

        when:
        place(game, p1, nearest)

        then:
        game.turnPhase == TurnPhase.WAITING_FOR_DIRECTION
        game.actionsFor(p1).contains(new ActionDefinition('chooseDirection', 'forward', 'backward'))

        when:
        directionPerformer.perform(game, p1, new StringAction('chooseDirection', direction))

        then:
        game.gamePlayer(p1).position == BoardPosition.of(expectedPosition)
        game.gamePlayer(p1).score == 1
        game.currentPlayer().same(p2)

        where:
        direction  | expectedPosition
        'forward'  | 12
        'backward' | 0
    }

    def 'invalid direction is rejected'() {
        given:
        def (game, p1, p2) = setupGame()
        def nearest = card('goToNearestScore', 100)
        game.gamePlayer(p1).giveCard(nearest)
        roll(game, p1, 3, 3)
        place(game, p1, nearest)

        when:
        directionPerformer.perform(game, p1, new StringAction('chooseDirection', 'sideways'))

        then:
        thrown(InvalidActionException)
        game.turnPhase == TurnPhase.WAITING_FOR_DIRECTION
    }

    // ---- lose cards ------------------------------------------------------------------------

    def 'a lose card can be played on the active player at any time in their turn'() {
        given:
        def (game, p1, p2) = setupGame(2, false)

        expect: 'before they roll'
        game.actionsFor(p2)*.name().contains('playLoseCard')
        !game.actionsFor(p1)*.name().contains('playLoseCard')

        when: 'and while they are choosing a card to place'
        roll(game, p1, 3, 4)

        then:
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT
        game.actionsFor(p2)*.name().contains('playLoseCard')
        !game.actionsFor(p1)*.name().contains('playLoseCard')
    }

    def 'rolling does not wait for other players to pass'() {
        given:
        def (game, p1, p2) = setupGame(2, false)

        when:
        roll(game, p1, 3, 4)

        then:
        game.turnPhase == TurnPhase.WAITING_FOR_CARD_PLACEMENT
        game.currentPlayer().same(p1)
    }

    def 'playing a lose card stops the turn and sends the active player to Lose'() {
        given:
        def (game, p1, p2) = setupGame(2, false)
        game.gamePlayer(p1).addScore(3)
        roll(game, p1, 3, 4)

        when:
        playLose(game, p2)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.gamePlayer(p1).score == 2
        !game.gamePlayer(p2).hasLoseCard()
        game.currentPlayer().same(p2)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'a lose card can be played before the active player has rolled'() {
        given:
        def (game, p1, p2) = setupGame(2, false)

        when:
        playLose(game, p2)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.currentPlayer().same(p2)
    }

    def 'a lose card played mid-turn drops the active player\'s pending choice'() {
        given:
        def (game, p1, p2) = setupGame(2, false)
        def exchange = card('exchangeCards', 100)
        game.placeCardOnSpace(BoardPosition.of(20), card('back3', 101))
        game.gamePlayer(p1).giveCard(exchange)
        roll(game, p1, 3, 4)
        place(game, p1, exchange)

        expect:
        game.turnPhase == TurnPhase.WAITING_FOR_EXCHANGE_CHOICE

        when:
        playLose(game, p2)

        then:
        game.currentPlayer().same(p2)
        game.turnPhase == TurnPhase.WAITING_FOR_ROLL
        game.exchangeCardSource == null
        !game.hasPlacedCardThisTurn()
    }

    def 'a lose card ends the turn even if another player is on the Lose space'() {
        given:
        def (game, p1, p2) = setupGame(2, false)
        game.gamePlayer(p2).position = BoardPosition.of(31)

        when:
        playLose(game, p2)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.currentPlayer().same(p2)
    }

    def 'with several players any of them with a lose card can play it'() {
        given:
        def (game, p1, p2, p3) = setupGame(3, false)
        roll(game, p1, 3, 4)

        expect:
        game.actionsFor(p2)*.name().contains('playLoseCard')
        game.actionsFor(p3)*.name().contains('playLoseCard')

        when:
        playLose(game, p3)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        !game.gamePlayer(p3).hasLoseCard()
        game.gamePlayer(p2).hasLoseCard()
        game.currentPlayer().same(p2)
    }

    def 'cannot play a lose card on yourself, twice, or without one'() {
        given:
        def (game, p1, p2) = setupGame(2, false)

        when:
        playLose(game, p1)

        then:
        thrown(InvalidActionException)

        when: 'p2 plays their only lose card, and then it is p2\'s turn so p1 plays theirs; p2 has none left'
        playLose(game, p2)
        playLose(game, p1)
        roll(game, p1, 3, 4)
        playLose(game, p2)

        then:
        thrown(InvalidActionException)
    }

    def 'cannot play a lose card when the game is over'() {
        given:
        def (game, p1, p2) = setupGame(2, false)
        game.gamePlayer(p1).addScore(11)
        game.gamePlayer(p1).position = BoardPosition.of(5)
        roll(game, p1, 3, 4)

        when:
        playLose(game, p2)

        then:
        thrown(InvalidActionException)
    }

    def 'the lose space ends the turn even if another player is on it'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p2).position = BoardPosition.of(31)
        game.gamePlayer(p1).position = BoardPosition.of(25)
        game.gamePlayer(p1).addScore(3)

        when:
        roll(game, p1, 3, 3)

        then:
        game.gamePlayer(p1).position == BoardPosition.of(31)
        game.gamePlayer(p1).score == 2
        game.currentPlayer().same(p2)
    }

    // ---- winning ---------------------------------------------------------------------------

    def 'reaching 12 points by landing on a score space wins'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).addScore(11)
        game.gamePlayer(p1).position = BoardPosition.of(5)

        when:
        roll(game, p1, 3, 4)

        then:
        game.status() == Game.Status.ENDED
    }

    def 'reaching 12 points by rolling a 12 wins'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).addScore(11)
        game.gamePlayer(p1).position = BoardPosition.of(1)

        when:
        roll(game, p1, 6, 6)

        then:
        game.status() == Game.Status.ENDED
    }

    def 'no rolling after the game has ended'() {
        given:
        def (game, p1, p2) = setupGame()
        game.gamePlayer(p1).addScore(11)
        game.gamePlayer(p1).position = BoardPosition.of(5)
        roll(game, p1, 3, 4)

        when:
        roll(game, p2, 3, 4)

        then:
        thrown(InvalidActionException)
    }

    // ---- views and definition --------------------------------------------------------------

    def 'player view shows own hand and lose card'() {
        given:
        def (game, p1, p2) = setupGame(2, false)

        when:
        def view = game.playerView(p1)

        then:
        view.myActionCards.size() == 4
        view.myLoseCard
        view.turnPhase == TurnPhase.WAITING_FOR_ROLL
    }

    def 'player view serialises positions and board cards as plain numbers'() {
        given:
        def (game, p1, p2) = setupGame()
        game.placeCardOnSpace(BoardPosition.of(20), card('back3', 101))

        when:
        def json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(game.playerView(p1)))

        then:
        json.get('myPosition').intValue() == 0
        json.get('boardCards').get('20').textValue() == 'back3_101'
    }

    def 'player view offers tied score spaces as forward then backward'() {
        given:
        def (game, p1, p2) = setupGame()
        def nearest = card('goToNearestScore', 100)
        game.gamePlayer(p1).giveCard(nearest)
        roll(game, p1, 3, 3)
        place(game, p1, nearest)

        when:
        def json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(game.playerView(p1)))

        then:
        json.get('nearestScoreOptions')*.intValue() == [12, 0]
    }

    def 'game definition registers every bonkers action'() {
        when:
        def names = new BonkersGameDefinition().actionPerformers()*.actionName()

        then:
        names.containsAll(['rollDice', 'placeCard', 'chooseExchangeCard', 'chooseDirection', 'playLoseCard'])
    }

    // ---- helpers ---------------------------------------------------------------------------

    /** Returns [game, p1, p2, ...]. By default every player's Lose card is spent so rolls don't open interrupt windows. */
    def setupGame(int playerCount = 2, boolean spendLoseCards = true) {
        def game = new BonkersGame(1, dice)
        def players = (1..playerCount).collect { new BasePlayer(it) }
        players.each { game.addPlayer(it) }
        players.each { game.playerReady(it) }
        if (spendLoseCards) {
            players.each { game.gamePlayer(it).useLoseCard() }
        }
        [game, *players]
    }

    def card(String type, int id) {
        new ActionCard(ActionCardType.fromCode(type), id)
    }

    def roll(BonkersGame game, player, int die1, int die2) {
        dice.queueRoll(die1, die2)
        rollPerformer.perform(game, player, new StringAction('rollDice', ''))
    }

    def place(BonkersGame game, player, ActionCard card) {
        placePerformer.perform(game, player, new StringAction('placeCard', card.shortCode()))
    }

    def playLose(BonkersGame game, player) {
        playLosePerformer.perform(game, player, new StringAction('playLoseCard', ''))
    }
}
