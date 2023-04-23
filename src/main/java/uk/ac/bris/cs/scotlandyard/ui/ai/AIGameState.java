package uk.ac.bris.cs.scotlandyard.ui.ai;

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
     * Gets the current location for Mr X.
     *
     * @return Current Mr X location for game state.
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

    List<Integer> getGameStateList ();
}
