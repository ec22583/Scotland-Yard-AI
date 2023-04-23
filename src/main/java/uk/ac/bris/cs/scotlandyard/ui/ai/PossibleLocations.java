package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Stores the current possible locations for Mr X.
 */
public interface PossibleLocations {
    /**
     * Generates the new possible locations based on the current locations and the new game state.
     * @param board Current Game Board.
     * @return updated (expanded) game board
     * @throws IllegalArgumentException if board is more than a turn into the future.
     */
    PossibleLocations updateLocations (Board board);

    ImmutableSet<Integer> getLocations ();

    /**
     * Gets the current turn from the class.
     * @return int of current turn
     */
    @Nonnull
    Integer getTurn ();
}
