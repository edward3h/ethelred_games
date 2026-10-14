package org.ethelred.games.bonkers

import spock.lang.Specification

class TestDeckAndPlayer extends Specification {
    def 'deck has two of each deck card type and nothing else'() {
        given:
        def deck = new ActionDeck(new Random(1))
        def cards = []
        while (deck.hasCards()) {
            cards << deck.draw()
        }

        expect:
        cards.size() == 40
        cards.groupBy { it.type() }.every { type, list -> list.size() == 2 }
        cards.groupBy { it.type() }.keySet() == ActionCardType.values().findAll { it.isInDeck() } as Set
        !cards.any { it.type() in [ActionCardType.GO_TO_LOSE, ActionCardType.BACK_15] }
        cards*.shortCode().toSet().size() == 40
    }

    def 'drawing from an empty deck returns null'() {
        given:
        def deck = new ActionDeck(new Random(1))
        40.times { deck.draw() }

        expect:
        !deck.hasCards()
        deck.remainingCards() == 0
        deck.draw() == null
    }

    def 'card codes round trip'() {
        given:
        def card = new ActionCard(ActionCardType.AHEAD_10, 7)

        expect:
        card.shortCode() == 'ahead10_7'
        ActionCard.fromCode('ahead10_7') == card
    }

    def 'movement amounts'() {
        expect:
        ActionCardType.AHEAD_6.movement == 6
        ActionCardType.BACK_12.movement == -12
        ActionCardType.BACK_15.movement == -15
        !ActionCardType.ROLL_AGAIN.isMovementAction()
    }

    def 'score is limited to 0..12'() {
        given:
        def player = new BonkersPlayer()

        when:
        player.addScore(-1)

        then:
        player.score == 0

        when:
        player.addScore(20)

        then:
        player.score == 12
        player.hasWon()
    }

    def 'new player starts at start with a lose card'() {
        given:
        def player = new BonkersPlayer()

        expect:
        player.position == 0
        player.score == 0
        player.hasLoseCard()
        !player.hasActionCards()
    }

    def 'taking and finding cards'() {
        given:
        def player = new BonkersPlayer()
        def card = new ActionCard(ActionCardType.BACK_3, 1)
        player.giveCard(card)

        expect:
        player.findCardByCode('back3_1') == card
        player.findCardByCode('back3_2') == null
        player.takeCard(card) == card
        player.takeCard(card) == null
        !player.hasActionCards()
    }
}
