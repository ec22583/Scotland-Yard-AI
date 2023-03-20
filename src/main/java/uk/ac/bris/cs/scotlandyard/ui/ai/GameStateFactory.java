package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public class GameStateFactory {
    //Helper method
    static private ImmutableMap<ScotlandYard.Ticket, Integer> getTicketsForPlayer (Board board, Piece piece) {
//      Get a TicketBoard of tickets
        Optional<Board.TicketBoard> ticketsOptional = board.getPlayerTickets(piece);
        if (ticketsOptional.isEmpty()) throw new IllegalArgumentException("Player does not exist.");
        Board.TicketBoard tickets = ticketsOptional.get();

        //      Generates map of ticket values from current TicketBoard state.
        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();

        //Go over through all ticket types
        for (ScotlandYard.Ticket ticketType : ScotlandYard.Ticket.values()) {
            ticketMap.put(ticketType, tickets.getCount(ticketType));
        }

        return ImmutableMap.copyOf(ticketMap);
    }

    //Helper function
    static private ImmutableList<Player> getDetectives (Board board){

        List<Player> detectives = new LinkedList<Player>(board
                .getPlayers()
                .stream()
                .filter(p -> p.isDetective())
                .map(piece -> {
                    Optional<Integer> locationOptional = board.getDetectiveLocation((Piece.Detective) piece);
                    if (locationOptional.isEmpty())
                        throw new IllegalStateException("Detective location not available.");

                    return new Player(
                                    piece,
                                    // Generates tickets for piece.
                                    GameStateFactory.getTicketsForPlayer(board, piece),
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

    public Board.GameState generateMrXGameState (Board board) {
        if (board.getAvailableMoves().isEmpty())
            throw new IllegalArgumentException("Board already winning board.");

        List<Boolean> moveSetup = new LinkedList<>();

//      Ensures that MrX is always visible to AI. (Himself)
        for (int i = 0; i < (board.getSetup().moves.size() - board.getMrXTravelLog().size()); i++) {
            moveSetup.add(true);
        }

        GameSetup gameSetup = new GameSetup(
                board.getSetup().graph,
                ImmutableList.copyOf(moveSetup)
        );

        int location = board
                .getAvailableMoves()
                .asList()
                .get(0)
                .source();


        Player mrX = new Player(MRX, GameStateFactory.getTicketsForPlayer(board, MRX) , location);
        ImmutableList<Player> detectives = GameStateFactory.getDetectives(board);

        MyGameStateFactory myGameStateFactory = new MyGameStateFactory();

        //Construct a new game state with parameters above
        Board.GameState gameState = myGameStateFactory.build(gameSetup, mrX, detectives);

        return gameState;
    }



    // Algorithm for possible new locations of MrX after his last definite location
    public List<Integer> generatePossibleNewLocations (
            ScotlandYard.Ticket usedTicket,
            List<Integer> detectiveLocations,
            List<Integer> oldPossibleLocations,
            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
        List<Integer> newLocations = new LinkedList<>();

        for (int oldPossibleLocation : oldPossibleLocations) {
            Set<EndpointPair<Integer>> edges = graph.edges();
            Set<EndpointPair<Integer>> possibleEdges =
                    ImmutableSet.copyOf(edges
                    .stream()
                    .filter(edge -> {
                        Optional<ImmutableSet<ScotlandYard.Transport>> optionalTransports = graph.edgeValue(edge);
                        if (optionalTransports.isEmpty()) throw new IllegalStateException("Edge does not exist");
                        ImmutableSet<ScotlandYard.Transport> transports = optionalTransports.get();
                        ImmutableSet<ScotlandYard.Ticket> tickets =
                                ImmutableSet.copyOf(transports
                                        .stream()
                                        .map(t -> t.requiredTicket())
                                        .toList());

                        return tickets.contains(usedTicket);
                    })

                    .toList());

            Set<Integer> possibleLocations = ImmutableSet.copyOf(possibleEdges
                    .stream()
                    .map(edge -> edge.adjacentNode(oldPossibleLocation))
                    .filter(l -> !detectiveLocations.contains(l))
                    .toList());

            newLocations.addAll(possibleLocations);
        }
        return newLocations;
    }

    public List<Board.GameState> generateDetectiveGameStates (Board board, List<Integer> possibleLocations){


//    TODO LIST
//        Get last known location of mrX (could be (start locations - detective starts) or last reveal log entry).
//        - Must store the start positions of the detectives at the start of the game.
//        Use an algorithm to deduce the possible locations of MrX from possible locations of last turn (see paper).
//              may also be best to just copy paste the getAvailableMoves code from the other part.

//        Store all possible locations for last turn.
//        Find the detective locations for the current turn.
//        Check the ticket used by the hider.
//        Use above data in the algorithm.
//        Update list of possible locations with new possible locations.
//        If MrX revealed, then update the list to the single known location
        // Repeat algorithm until next definite location.

        return null; //To do when we implement the Detectives AI
    }
}
