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
     * @return Current setup for game state.
     */
    @Nonnull
    @Override
    GameSetup getSetup();

    /**
     * Getter method
     *
     * @return Set of all player pieces (not players)
     */
    @Nonnull
    @Override
    ImmutableSet<Piece> getPlayers();

    /**
     * Gets the location for a detective if detective exists.
     *
     * @param detective Piece for a detective
     * @return Optional. Returns int of location if detective exists and empty if no detective exists.
     */
    @Nonnull
    @Override
    Optional<Integer> getDetectiveLocation(Piece.Detective detective);

    /**
     * @param piece Piece for a player.
     * @return If player exists, TicketBoard for corresponding player, else empty if no matching player.
     */
    @Nonnull
    @Override
    Optional<TicketBoard> getPlayerTickets(Piece piece);

    /**
     * Getter method
     *
     * @return Current log for game turn
     */
    @Nonnull
    @Override
    ImmutableList<LogEntry> getMrXTravelLog();

    /**
     * Getter method
     *
     * @return Set of winners for current game state.
     */
    @Nonnull
    @Override
    ImmutableSet<Piece> getWinner();

    /**
     * Getter method
     *
     * @return Previous move carried out in this game.
     */
    Optional<Move> getPreviousMove();

    /**
     * Gets all possible moves for all remaining players for a turn
     *
     * @return Set of all moves that can be carried out this turn
     */
    @Nonnull
    @Override
    ImmutableSet<Move> getAvailableMoves();

    /**
     * Advances game to the next turn from passed in move
     *
     * @param move The chosen move to carry out
     * @return The new game state from after move carried out
     */
    @Nonnull
    @Override
    AIGameState advance(Move move);
}
