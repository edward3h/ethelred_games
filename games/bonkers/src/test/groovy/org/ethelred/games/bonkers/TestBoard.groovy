package org.ethelred.games.bonkers

import spock.lang.Specification

class TestBoard extends Specification {
    def 'board has the layout described in the rules'() {
        expect:
        Board.TOTAL_SPACES == 55
        Board.getSpaceType(position) == type

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
        Board.isScoreSpace(0)
        Board.isScoreSpace(12)
        Board.isScoreSpace(24)
        !Board.isScoreSpace(31)
        Board.isLoseSpace(31)
        !Board.isLoseSpace(0)
    }

    def 'fixed actions'() {
        expect:
        Board.getFixedAction(35) == ActionCardType.GO_TO_LOSE
        Board.getFixedAction(49) == ActionCardType.GO_TO_LOSE
        Board.getFixedAction(43) == ActionCardType.BACK_15
        Board.getFixedAction(10) == null
    }

    def 'positions wrap round the loop'() {
        expect:
        Board.normalizePosition(input) == expected
        Board.move(from, spaces) == expected

        where:
        input | from | spaces | expected
        55    | 50   | 5      | 0
        56    | 50   | 6      | 1
        -1    | 3    | -4     | 54
        -12   | 5    | -17    | 43
    }

    def 'nearest score from #from'() {
        expect:
        Board.findNearestScorePositions(from) == expected as int[]

        where:
        from | expected
        5    | [0]
        7    | [12]
        10   | [12]
        20   | [24]
        31   | [24]
        40   | [0]
        54   | [0]
        // ties: [forward, backward]
        6    | [12, 0]
        18   | [24, 12]
    }
}
