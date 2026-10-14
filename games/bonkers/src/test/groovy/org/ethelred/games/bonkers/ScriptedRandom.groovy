package org.ethelred.games.bonkers

/**
 * Random that returns queued die faces (1-6) from nextInt(6), falling back to a seeded sequence when empty.
 */
class ScriptedRandom extends Random {
    private final LinkedList<Integer> faces = new LinkedList<>()

    ScriptedRandom() {
        super(42L)
    }

    void queueRoll(int die1, int die2) {
        faces.add(die1 - 1)
        faces.add(die2 - 1)
    }

    @Override
    int nextInt(int bound) {
        faces.isEmpty() ? super.nextInt(bound) : faces.removeFirst()
    }
}
