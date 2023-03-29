package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface AIGameState extends Board.GameState {
    /**
     * Getter method
     *
     * @return Previous move carried out in this game.
     */
    Optional<Move> getPreviousMove();

    /**
     * Advances the game state to the new game state based on the input move.
     * @param move The move to carry out
     * @return The updated game state
     */
    @Override
    @Nonnull
    AIGameState advance(Move move);
}
