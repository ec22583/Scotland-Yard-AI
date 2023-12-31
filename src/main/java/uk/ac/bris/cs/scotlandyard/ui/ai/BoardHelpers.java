package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Series of Helper functions for the Board used in multiple class files
 * */
public interface BoardHelpers {

    /**
     * Possible starting locations of all players.
     */
    int[] START_LOCATIONS = {35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172};

    @Nonnull
    static ImmutableList<Player> getDetectives (Board board){

        List<Player> detectives = new LinkedList<>(board
                .getPlayers()
                .stream()
                .filter(Piece::isDetective)
                .map(piece -> {
                            Optional<Integer> locationOptional = board.getDetectiveLocation((Piece.Detective) piece);
                            if (locationOptional.isEmpty())
                                throw new IllegalStateException("Detective location not available.");

                            return new Player(
                                    piece,
                                    // Generates tickets for piece.
                                    BoardHelpers.getTicketsForPlayer(board, piece),
                                    //  Piece must be cast to a Detective. Not an issue since mrx filtered out earlier
                                    // (For type safety). .get() fine as piece always is a detective.
                                    locationOptional.get()
                            );
                        }
                )
                .toList()
        );
        return ImmutableList.copyOf(detectives);
    }


    /**
     * Gets the locations of all detectives from the current board
     * @apiNote If possible, use {@link AIGameState#getDetectiveLocations()} as it is faster
     * @param board Current Game State
     * @return List of locations for detectives
     * @throws IllegalArgumentException if a location for a detective cannot be found.
     */
    @Nonnull
    static List<Integer> getDetectiveLocations (Board board) {
        List<Piece> detectives = board.getPlayers().stream().filter(Piece::isDetective).toList();
        return detectives
                .stream()
                .map((d) -> {
                    Optional<Integer> optionalLocation = board.getDetectiveLocation((Piece.Detective) d);
                    if (optionalLocation.isEmpty()) throw new IllegalArgumentException("No location found");
                    return optionalLocation.get();
                })
                .toList();
    }

    /**
     * Gets a map of tickets and their corresponding total for player.
     * @param board Current Game State
     * @param piece Piece to get tickets of
     * @return A map of tickets and corresponding number owned
     * @throws IllegalArgumentException if player doesn't exist
     */
    static ImmutableMap<ScotlandYard.Ticket, Integer> getTicketsForPlayer (Board board, Piece piece) {
//      Get a TicketBoard of tickets
        Optional<Board.TicketBoard> ticketsOptional = board.getPlayerTickets(piece);
        if (ticketsOptional.isEmpty()) throw new IllegalArgumentException("Player does not exist.");
        Board.TicketBoard tickets = ticketsOptional.get();

//      Generates map of ticket values from current TicketBoard state.
        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();

//      Go over through all ticket types
        for (ScotlandYard.Ticket ticketType : ScotlandYard.Ticket.values()) {
            ticketMap.put(ticketType, tickets.getCount(ticketType));
        }

        return ImmutableMap.copyOf(ticketMap);
    }
}
