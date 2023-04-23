package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface AIGameState extends Board.GameState {
    /**
     * @return Previous move carried out in this game.
     */
    Optional<Move> getPreviousMove();

    /**
     * @return List of all detective locations for the game state.
     */
    List<Integer> getDetectiveLocations();

    /**
     * @return Current Mr X location for game state.
     */
    int getMrXLocation();

    /**
     * Advances the game state to the new game state based on the input move.
     * @param move The move to carry out on current game state
     * @return The updated game state after move.
     */
    @Override @Nonnull
    AIGameState advance(Move move);

    /**
     * Used to create a dataset from the current game state.
     * @return List of information about current game state:
     * mrxlocation, detectivelocation1, detectivelocation2, detectivelocation3, detectivelocation4, detectivelocation5,
     * detective1taxi, detective1bus, detective1train, detective2taxi, detective2bus, detective2train, detective3taxi,
     * detective3bus, detective3train, detective4taxi, detective4bus, detective4train, detective5taxi,
     * detective5bus, detective5train, mrxtaxi, mrxbus, mrxtrain, mrxdouble, mrxsecret, turnsleft, newmrxlocation
     */
    List<Integer> getGameStateList ();
}
