package org.ethelred.games.bonkers

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class TestBoardPosition extends Specification {
    def 'board has the layout described in the rules'() {
        expect:
        BoardPosition.SPACES == 55
        BoardPosition.of(position).spaceType() == type

        where:
        position | type
        0        | SpaceType.START_SCORE
        1        | SpaceType.BLANK
        11       | SpaceType.BLANK
        12       | SpaceType.SCORE
        13       | SpaceType.BLANK
        24       | SpaceType.SCORE
        25       | SpaceType.BLANK
        30       | SpaceType.BLANK
        31       | SpaceType.LOSE
        32       | SpaceType.BLANK
        34       | SpaceType.BLANK
        35       | SpaceType.FIXED_ACTION
        36       | SpaceType.BLANK
        42       | SpaceType.BLANK
        43       | SpaceType.FIXED_ACTION
        44       | SpaceType.BLANK
        48       | SpaceType.BLANK
        49       | SpaceType.FIXED_ACTION
        50       | SpaceType.BLANK
        54       | SpaceType.BLANK
    }

    def 'start counts as a score space'() {
        expect:
        BoardPosition.START.isScore()
        BoardPosition.of(12).isScore()
        BoardPosition.of(24).isScore()
        !BoardPosition.LOSE.isScore()
        BoardPosition.LOSE.isLose()
        !BoardPosition.START.isLose()
    }

    def 'fixed actions'() {
        expect:
        BoardPosition.of(35).fixedAction() == ActionCardType.GO_TO_LOSE
        BoardPosition.of(49).fixedAction() == ActionCardType.GO_TO_LOSE
        BoardPosition.of(43).fixedAction() == ActionCardType.BACK_15
        BoardPosition.of(10).fixedAction() == null
    }

    def 'positions wrap round the loop'() {
        expect:
        BoardPosition.of(input).index() == expected
        BoardPosition.of(from).plus(spaces).index() == expected

        where:
        input | from | spaces | expected
        55    | 50   | 5      | 0
        56    | 50   | 6      | 1
        -1    | 3    | -4     | 54
        -12   | 5    | -17    | 43
    }

    def 'equivalent positions are equal'() {
        expect:
        BoardPosition.of(62) == BoardPosition.of(7)
        BoardPosition.of(-48) == BoardPosition.of(7)
    }

    def 'nearest score from #from'() {
        when:
        def nearest = BoardPosition.of(from).nearestScores()

        then:
        !nearest.isTie()
        nearest.nearest() == BoardPosition.of(expected)

        where:
        from | expected
        5    | 0
        7    | 12
        10   | 12
        20   | 24
        31   | 24
        40   | 0
        54   | 0
    }

    def 'nearest score from #from is a tie'() {
        when:
        def nearest = BoardPosition.of(from).nearestScores()

        then:
        nearest.isTie()
        nearest.forward() == BoardPosition.of(forward)
        nearest.backward() == BoardPosition.of(backward)

        when:
        nearest.nearest()

        then:
        thrown(IllegalStateException)

        where:
        from | forward | backward
        6    | 12      | 0
        18   | 24      | 12
    }

    def 'serialises as a plain number'() {
        given:
        def mapper = new ObjectMapper()

        expect:
        mapper.writeValueAsString(BoardPosition.of(7)) == '7'
        mapper.readValue('62', BoardPosition) == BoardPosition.of(7)
        mapper.writeValueAsString([(BoardPosition.of(7)): 'x']) == '{"7":"x"}'
    }
}
