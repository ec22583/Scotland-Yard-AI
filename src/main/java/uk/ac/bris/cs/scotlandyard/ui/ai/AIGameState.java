package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface AIGameState extends Board.GameState {
    /**
     * Getter method
     *
     * @return Previous move carried out in this game.
     */
    Optional<Move> getPreviousMove();

    /**
     * Gets all the locations of the detectives for the current game state
     *
     * @return List of all detective locations for the game state.
     */
    List<Integer> getDetectiveLocations();

    /**
     * Gets MrX's current location for the AI.
     * @return Mr X's current position.
     */
    int getMrXLocation();

    /**
     * Advances the game state to the new game state based on the input move.
     * @param move The move to carry out
     * @return The updated game state
     */
    @Override
    @Nonnull
    AIGameState advance(Move move);
}
