# Bonkers Game Details

"Bonkers" is a board game for 2 to 4 players, published by Parker Brothers in 1978.

The official rules are in `references/Bonkers.pdf`, and photographs of the board and cards are alongside it.
This document restates those rules as the game engine implements them. It uses "action card" where the official
rules say "Track Card", and "space" for a position on the board.

## Components

### Dice

2 standard 6 sided dice.

### Board

The board has a loop of 55 spaces. In order, starting from Start (position 0):

* Start (also counts as Score)
* 11 blank spaces
* Score
* 11 blank spaces
* Score
* 6 blank spaces
* Lose
* 3 blank spaces
* Go To Lose (action)
* 7 blank spaces
* Back 15 (action)
* 5 blank spaces
* Go To Lose (action)
* 5 blank spaces

The three types of space in the official rules are Lose, Score and "track" (everything else). The Go To Lose and
Back 15 spaces have a card printed on the board, which is not described in the PDF; they are taken from the board
photograph. They behave like an action card that is always on the space.

### Lose Cards

4 large cards saying "Go To Lose", one for each player.

### Action Cards

2 each of the following cards, 40 in all:

* Go To Start
* Go To Nearest Score
* Roll Again
* Exchange Cards
* Ahead 1
* Ahead 2
* Ahead 3
* Ahead 4
* Ahead 5
* Ahead 6
* Ahead 10
* Ahead 12
* Back 1
* Back 2
* Back 3
* Back 4
* Back 5
* Back 6
* Back 10
* Back 12

### Player Pieces

4 player pieces, 1 each of colours red, green, yellow, blue.

### Scoreboard

Tracks scores from 0 to 12 for each player. A score can never go below 0.

## Definitions

* "Roll the dice" means roll both dice and add the 2 numbers together, resulting in a total from 2 to 12.
* "Score a point" means add 1 to the player's score on the scoreboard.
* "Lose a point" means subtract 1 from the player's score on the scoreboard.
* A space is "occupied" if another player's piece is on it.

## Setup

* Each player chooses one of the player pieces and places it on the Start space.
* Each player is given 1 Lose card.
* The scoreboard is set to 0 for each player.
* The action cards are shuffled.
  * 4 are dealt to each player. The players put these cards face-up in front of them.
  * The rest of the cards are in a face-down deck.
* Each player rolls the dice. The player with the highest total takes the first turn, and play then proceeds to
  the left.

## Player Turn

Roll the dice and move the piece that many spaces ahead. What happens next depends on the space the piece lands on.

* **Double 6** (a total of 12): the player scores a point, and moves 12 spaces ahead. Then carry on as for any
  other landing.
* **Occupied track space**: roll again. Do not place a card, and if there is already a card on the space, do not
  follow it.
* **Score space** (including Start): the player scores a point. If the space is occupied they roll again,
  otherwise the turn ends.
* **Lose space**: the player loses a point and the turn ends, even if the space is occupied.
* **Unoccupied track space with no card on it**: the player places one of their action cards on it, and
  immediately follows that card.
* **Unoccupied track space with a card already on it**: follow that card.

After following a card, apply the same rules to wherever the piece ends up. Keep moving until the piece lands on:

1. a second unoccupied track space with no card on it (the turn ends), or
2. an unoccupied Score space (the turn ends), or
3. the Lose space (the turn ends).

A player may place at most one card on the board in a turn. When they place a card, they take another from the deck
if there is one. When the deck has run out, play continues with the cards the players hold.

### Cards

* **Go To Start**: move to the Start space.
* **Go To Nearest Score**: move to the nearest Score space, counting spaces in either direction. If two are the
  same distance away the player chooses which.
* **Go To Lose**: move to the Lose space.
* **Roll Again**: roll again.
* **Ahead n** and **Back n**: move that many spaces forward or backward.
* **Exchange Cards**: the player *may* take any other card on the board, move it to their space and put the
  Exchange Cards card where it was. They then follow the moved card. If there is no other card on the board, or
  the player declines, nothing is exchanged and the turn ends.

### Entrapment

If the cards send the piece round in a circle, for example landing on a "Back 3" that sends the piece to an
"Ahead 3", the player does not keep moving back and forth. Instead they score a point, the piece stays on the
space with the forward card, and the turn ends.

The official rules only describe this two card case. For a longer circle (it can include the Back 15 space), the
implementation leaves the piece on the last forward space it visited.

## Lose Card

* A player with a Lose card can use it on another player at any time during that player's turn.
* The turn stops immediately. The other player moves their piece to the Lose space, loses a point, and the dice
  pass to the next player. This is the case even if the Lose space is occupied.
* The Lose card is then discarded.

In the implementation, "any time" means whenever the game is waiting for the active player to act. That includes
before they roll, and while they are choosing a card to place or an exchange.

## Ending the game

As soon as a player gets a score of 12, they have won! The game ends.

## Differences from the official rules

* The first player is the first to join, not the highest roll of the dice.
* A player takes their replacement card straight after placing a card, rather than when their turn ends.
