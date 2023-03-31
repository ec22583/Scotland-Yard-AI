package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import jakarta.websocket.Endpoint;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.MRX_LOCATIONS;

public class PossibleLocationsFactory {

    //Thing to produce
    private final class MyPossibleLocations implements PossibleLocations {

        private final List<Integer> locations;
        private final int turn;

        public MyPossibleLocations(Collection<Integer> locations, int turn){
            this.locations = ImmutableList.copyOf(locations);
            this.turn = turn;
        }

        /**
         * Helper function to generatePossibleNewLocations. Takes an edge of the graph and sees if it uses a
         * specific given ticket type
         * @param edge edge to be checked
         * @param usedTicket ticket type to check against the edge
         * @param graph graph of the game
         * @return a predicate whether the edge uses that ticket type
         * @throws IllegalStateException if the edge doesn't exist
         * */
        private boolean checkEdgeUsesTicket (EndpointPair<Integer> edge,
                                                 ScotlandYard.Ticket usedTicket,
                                                 ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
            //Extract type of transport that is used to go to the edge
            Optional<ImmutableSet<ScotlandYard.Transport>> optionalTransports = graph.edgeValue(edge);
            if (optionalTransports.isEmpty()) throw new IllegalStateException("Edge does not exist");

            ImmutableSet<ScotlandYard.Transport> transports = optionalTransports.get();
            ImmutableSet<ScotlandYard.Ticket> tickets =
                    ImmutableSet.copyOf(
                            transports
                            .stream()
                            .map(t -> t.requiredTicket())
                            .toList());

            return tickets.contains(usedTicket); //returns true if edge contains ticket of specific type
        }

        /**
         * Algorithm for possible new locations of MrX after his last definite location
         * Helper function for generateDetectiveGameStates
         * @param usedTicket ticket used by MrX
         * @param detectiveLocations list of locations of the detectives
         * @param oldPossibleLocations old possible locations of MrX calculated in previous turns
         * @param graph Graph of the game
         * @return updated list of possible locations of MrX
         * */
        private List<Integer> generatePossibleNewLocations (
                ScotlandYard.Ticket usedTicket,
                List<Integer> detectiveLocations,
                List<Integer> oldPossibleLocations,
                ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {
            List<Integer> newLocations = new LinkedList<>();

            for (int oldPossibleLocation : oldPossibleLocations) {
                Set<EndpointPair<Integer>> edges = graph.incidentEdges(oldPossibleLocation);
                Set<EndpointPair<Integer>> possibleLocations;
                if (usedTicket.equals(ScotlandYard.Ticket.SECRET)) {
                    possibleLocations = ImmutableSet.copyOf(edges); //Anywhere can be accessed using secret tickets
                }
                else {
                    possibleLocations = ImmutableSet.copyOf(edges
                            .stream()
                            .filter(edge -> checkEdgeUsesTicket(edge, usedTicket, graph))
                            .toList()
                    );
                }

                newLocations.addAll(possibleLocations
                        .stream()
                        // Adjacent node from old possible locations
                        .map(edge -> edge.adjacentNode(oldPossibleLocation))
                        //Prune all possible locations that detectives are in
                        .filter(l -> !detectiveLocations.contains(l))
                        .toList());
            }
            return newLocations;
        }

        @Nonnull
        /**
         * Helper function to updateLocations
         * @param locations locations to filter out
         * @param detectiveLocations detective locations to be filtered out of locations
         * */
        private List<Integer> filterDetectiveLocationsfromLocations(List<Integer> locations,
                                                                    List<Integer> detectiveLocations){
            return locations
                    .stream()
                    .filter((location) -> !detectiveLocations.contains(location))
                    .toList();
        }

        @Nonnull
        /**
         * Generate new possible locations based on MrX's ticket (from the log entry)
         * Clears the old possible locations if it is a revealing turn
         * */
        private List<Integer> newLocationsFromLogEntry (LogEntry logEntry,
                                                        List<Integer> newLocations,
                                                        Board board,
                                                        List<Integer> detectiveLocations){
            //If revealing turn
            if (logEntry.location().isPresent()) {
                newLocations = new ArrayList<>(
                        ImmutableList.of( logEntry.location().get() )
                );
            }
            else {
                newLocations = this.generatePossibleNewLocations(
                        logEntry.ticket(),
                        detectiveLocations,
                        newLocations,
                        board.getSetup().graph
                );
            }
            return newLocations;
        }

        @Override @Nonnull
        public MyPossibleLocations updateLocations (Board board) {
            if (board.getMrXTravelLog().size() - this.turn > 2){
                throw new IllegalArgumentException("Can't go further than a double move");
            }

            List<Integer> detectiveLocations = BoardHelpers.getDetectiveLocations(board);
            List<Integer> newLocations = filterDetectiveLocationsfromLocations(this.getLocations(), detectiveLocations);

            if (board.getMrXTravelLog().size() == this.turn) {
                return new MyPossibleLocations(newLocations, board.getMrXTravelLog().size());
//              If MrX hasn't moved, just filter out any current detective locations from possible locations.
            }
            else {
//              Mr X has moved, so generate new possible locations and filter out any which detectives are in
                List<LogEntry> logEntries = board.getMrXTravelLog()
                        .subList(this.turn, board.getMrXTravelLog().size());
                for (LogEntry logEntry : logEntries) {
                    newLocations = newLocationsFromLogEntry(logEntry, newLocations, board, detectiveLocations);
                }
            }
            return new MyPossibleLocations(newLocations, board.getMrXTravelLog().size());
        }

        @Override @Nonnull
        public Integer getTurn(){
            return this.turn;
        }

        public ImmutableList<Integer> getLocations () {
            return ImmutableList.copyOf(this.locations);
        }

    }

    /**
     * Returns the initial possible locations from the initial board state.
     * @param board Current Game Board (must be initial board)
     * @return Initial Possible Locations.
     */
    public MyPossibleLocations buildFromInitialBoard (Board board) {
        List<Integer> possibleLocations = new LinkedList<>(MRX_LOCATIONS);
        List<Integer> detectiveLocations = BoardHelpers.getDetectiveLocations(board);

        //Prune detective locations
        possibleLocations.removeAll(detectiveLocations);

        //Make possible locations for first turn
        return new MyPossibleLocations(possibleLocations, 0);
    }
}
