package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.annotation.Nonnull;
import java.util.List;

public interface PossibleLocations {
//  Creates a new PossibleLocations based on the ticket(s) used by MrX.
//  Pre-condition: Must only be a single turn (single move/double move) from previous state.

    /**
     * Generates the new possible locations based on the current locations and the new game state.
     * @param board Current Game Board.
     * @return updated (expanded) game board
     * @throws IllegalArgumentException if board is more than a turn into the future.
     */
    PossibleLocations updateLocations (Board board);

    /**
     * When MrX surfaces on a revealing round, clear previous possible locations and add his definite location
     * @param  board The current game board
     * @param  turn The turn the reveal is on
     * @return List of length 1 of the possible locations
     * @throws IllegalArgumentException If attempting to set a new known location on a hidden move
     * */
    PossibleLocations newKnownLocation (Board board, int turn);

    List<Integer> getLocations ();

    /**
     * Gets the current turn from the class.
     * @return int of current turn
     */
    @Nonnull
    Integer getTurn ();
}
