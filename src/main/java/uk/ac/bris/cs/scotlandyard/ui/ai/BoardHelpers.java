package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.*;

@SuppressWarnings("ALL")
public interface BoardHelpers {
    /**
     * Possible starting locations of all players.
     */
    int[] START_LOCATIONS = {35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172};

    /**
     * Gets the locations of all detectives from the current board.
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
     * Gets a map of tickets and the number of owned tickets by player.
     * @param board Current Game State
     * @param piece Piece to get tickets of
     * @return A map of tickets and the number owned
     * @throws IllegalArgumentException if player doesn't eixst
     */
    static ImmutableMap<ScotlandYard.Ticket, Integer> getTicketsForPlayer (Board board, Piece piece) {
//      Get a TicketBoard of tickets
        Optional<Board.TicketBoard> ticketsOptional = board.getPlayerTickets(piece);
        if (ticketsOptional.isEmpty()) throw new IllegalArgumentException("Player does not exist.");
        Board.TicketBoard tickets = ticketsOptional.get();

        // Generates map of ticket values from current TicketBoard state.
        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();

        //Go over through all ticket types
        for (ScotlandYard.Ticket ticketType : ScotlandYard.Ticket.values()) {
            ticketMap.put(ticketType, tickets.getCount(ticketType));
        }

        return ImmutableMap.copyOf(ticketMap);
    }

    /**
     * Returns an updated game setup with Mr X's moves revealed so that it is easier to track for the AI.
     * @param board Current Game State
     * @return gameSetup Game setup with all of Mr X's moves revealed.
     */
    static GameSetup generateGameSetup (Board board) {
        List<Boolean> moveSetup = new LinkedList<>();

//      Ensures that MrX is always visible to himself
        for (int i = 0; i < (board.getSetup().moves.size() - board.getMrXTravelLog().size()); i++) {
            moveSetup.add(true);
        }

        return new GameSetup(
                board.getSetup().graph,
                ImmutableList.copyOf(moveSetup)
        );
    }
}
