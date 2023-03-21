package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.*;

public class PossibleLocationsFactory {

    //Thing to produce
    private final class MyPossibleLocations implements PossibleLocations {

        private final List<Integer> locations;
        private final int turn;

        public MyPossibleLocations(Collection<Integer> locations, int turn){
            this.locations = ImmutableList.copyOf(locations);
            this.turn = turn;
        }

        // Algorithm for possible new locations of MrX after his last definite location
        // Helper function for generateDetectiveGameStates

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
                    possibleLocations = ImmutableSet.copyOf(edges);
                }
                else {
                    possibleLocations = ImmutableSet.copyOf(edges
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

                                return tickets.contains(usedTicket); //turns all tickets of a specific type
                            })
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

        @Override @Nonnull
        public MyPossibleLocations updateLocations (Board board) {

            List<ScotlandYard.Ticket> mrXTickets = new ArrayList<>(2);
            if (board.getMrXTravelLog().size() - this.turn + 1 > 2){
                throw new IllegalArgumentException("Can't go further than a double move");
            }

            for (int i = this.turn + 1; i < board.getMrXTravelLog().size(); i++) {
                mrXTickets.add(board.getMrXTravelLog().get(i).ticket());
            }

            List<Integer> detectiveLocations = BoardHelpers.getDetectiveLocations(board);
            List<Integer> newLocations = this.locations;

            // Generate new possible moves based on MrX's ticket
            // the method already filters out detective locations
            for (ScotlandYard.Ticket ticket : mrXTickets) {
                newLocations = this.generatePossibleNewLocations(
                        ticket,
                        detectiveLocations,
                        newLocations,
                        board.getSetup().graph
                );
            }

            return new MyPossibleLocations(newLocations , board.getMrXTravelLog().size() - 1);
        }

        @Override @Nonnull
        public MyPossibleLocations newKnownLocation (Board board) {
            LogEntry revealEntry = board.getMrXTravelLog().get(board.getMrXTravelLog().size() - 1);
            if (revealEntry.location().isEmpty())
                throw new IllegalArgumentException("Trying to set new known location on hidden move");

            int newTurn = board.getMrXTravelLog().size() - 1;

            MyPossibleLocations newPossibleLocations = new MyPossibleLocations(
                    List.of(revealEntry.location().get()), newTurn);

            return newPossibleLocations;
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
        List<Integer> possibleLocations = new LinkedList<>();
        Arrays.stream( BoardHelpers.START_LOCATIONS )
                .forEach(l -> possibleLocations.add(l));

        List<Integer> detectiveLocations = BoardHelpers.getDetectiveLocations(board);

        //Prune detective locations
        possibleLocations.removeAll(detectiveLocations);

        //Make possible locations for first turn
        return new MyPossibleLocations(possibleLocations, 0);
    }
}
