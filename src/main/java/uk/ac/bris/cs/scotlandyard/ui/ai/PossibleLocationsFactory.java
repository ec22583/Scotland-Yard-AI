package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.MRX_LOCATIONS;

public class PossibleLocationsFactory {

    //Thing to produce
    private final class MyPossibleLocations implements PossibleLocations {

        private final ImmutableSet<Integer> locations;
        private final int turn;

        public MyPossibleLocations(Collection<Integer> locations, int turn){
            this.locations = ImmutableSet.copyOf(locations);
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
        private static boolean checkEdgeUsesTicket (EndpointPair<Integer> edge,
                                                 ScotlandYard.Ticket usedTicket,
                                                 ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
            //Extract type of transport that is used to go to the edge
            ImmutableSet<ScotlandYard.Transport> transports = graph.edgeValue(edge).orElseThrow();
            List<ScotlandYard.Ticket> tickets = transports
                    .stream()
                    .map(t -> t.requiredTicket())
                    .toList();

            return tickets.contains(usedTicket); //returns true if edge contains ticket of specific type
        }

        /**
         * Algorithm for possible new locations of MrX from current possible locations.
         * Helper function for generateDetectiveGameStates
         * @param usedTicket ticket used by MrX
         * @param detectiveLocations list of locations of the detectives
         * @param oldPossibleLocations old possible locations of MrX calculated in previous turns
         * @param graph Graph of the game
         * @return updated list of possible locations of MrX
         * */
        private static ImmutableSet<Integer> generatePossibleNewLocations (
                ScotlandYard.Ticket usedTicket,
                Collection<Integer> detectiveLocations,
                Collection<Integer> oldPossibleLocations,
                ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph) {

            ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
            Set<Integer> detectiveLocationsSet = ImmutableSet.copyOf(detectiveLocations);

            for (int oldPossibleLocation : oldPossibleLocations) {
                builder.addAll(
//              Gets all edges which connect to the current oldPossibleLocation.
                        graph.incidentEdges(oldPossibleLocation)
                        .stream()

//                      Filters edges so only the ones which use the correct ticket are included
                        .filter(edge -> usedTicket.equals(ScotlandYard.Ticket.SECRET) ||
                                checkEdgeUsesTicket(edge, usedTicket, graph))

//                      Gets the new locations from the edge and old location.
                        .map(edge -> edge.adjacentNode(oldPossibleLocation))

                        //Prune all possible locations that detectives are in
                        .filter(l -> !detectiveLocationsSet.contains(l))
                        .toList());
            }
            return builder.build();
        }

        @Nonnull
        /**
         * Helper function to updateLocations
         * @param locations locations to filter out
         * @param detectiveLocations detective locations to be filtered out of locations
         * */
        private static ImmutableSet<Integer> filterDetectiveLocationsFromLocations(Collection<Integer> locations,
                                                                    Collection<Integer> detectiveLocations){
            return ImmutableSet.copyOf(locations
                    .stream()
                    .filter((location) -> !detectiveLocations.contains(location))
                    .toList());
        }

        @Nonnull
        /**
         * Generate new possible locations based on MrX's ticket (from the log entry)
         * Clears the old possible locations if it is a revealing turn
         * */
        private ImmutableSet<Integer> newLocationsFromLogEntry (LogEntry logEntry,
                                                        Collection<Integer> newLocations,
                                                        Board board,
                                                        List<Integer> detectiveLocations){
            //If revealing turn
            if (logEntry.location().isPresent()) {
                return ImmutableSet.of(logEntry.location().get());
            }
            else {
                return MyPossibleLocations.generatePossibleNewLocations(
                        logEntry.ticket(),
                        detectiveLocations,
                        newLocations,
                        board.getSetup().graph
                );
            }
        }

        @Override @Nonnull
        public MyPossibleLocations updateLocations (Board board) {
            if (board.getMrXTravelLog().size() - this.turn > 2){
                throw new IllegalArgumentException("Can't go further than a double move");
            }

            List<Integer> detectiveLocations;
            if (AIGameState.class.isInstance(board)) {
                detectiveLocations = ((AIGameState) board).getDetectiveLocations();
            } else {
                detectiveLocations = BoardHelpers.getDetectiveLocations(board);
            }

            Set<Integer> newLocations = filterDetectiveLocationsFromLocations(this.getLocations(), detectiveLocations);

            if (board.getMrXTravelLog().size() > this.turn) {
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

        public ImmutableSet<Integer> getLocations () {
            return this.locations;
        }

    }

    /**
     * Returns the initial possible locations for Mr X.
     * @return Initial Possible Locations.
     */
    public MyPossibleLocations buildInitialLocations() {
        //Make possible locations for first turn
        return new MyPossibleLocations(MRX_LOCATIONS, 0);
    }
}
