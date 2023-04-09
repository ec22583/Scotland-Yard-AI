package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//Wrapper class for all Heuristics (interfaces)
public interface Heuristics {


    /**
     * Apply rules to improve MrX's use of secret tickets (Ticket economy)
     * */
    class MoveFiltering {

        public final int REMOVE_FROM_FIRST_TWO_ROUNDS = 0;
        public final int REMOVE_FROM_REVEALING_ROUND = 1;
        public final int ALL_POSSIBLE_LOCATIONS_HAVE_TAXIS = 2;

        public interface FilterStrategy {
            /**
             * Used to filter out moves. True keeps a move and false removes a move.
             * @param move Move to check
             * @param gameState Game state used for context
             * @return Boolean of whether move should be kept
             */
            boolean execute (Move move, AIGameState gameState);
        }

        public class RemoveFromFirstTwoRounds implements FilterStrategy{
            @Override @Nonnull
            public boolean execute (Move move, AIGameState gameState){
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

                if (tickets.contains(ScotlandYard.Ticket.SECRET) &&
                        gameState.getMrXTravelLog().size() <= 2 ) return false;
                else return true;
            }
        }

        public class RemoveFromRevealingRound implements FilterStrategy{
            @Override @Nonnull
            public boolean execute (Move move, AIGameState gameState){
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

//              Check if move at current turn is a reveal move
                if (tickets.contains(ScotlandYard.Ticket.SECRET) && gameState
                        .getSetup()
                        .moves
                        .get(gameState.getMrXTravelLog().size() - 1) // get starts at index 0
                        .equals(true)
                        ) {
                    return false;
                }
                else {
                    return true;
                }
            }
        }

        /**
         * @throws IllegalArgumentException if edge on a graph isn't found
         * */
        public class AllPossibleLocationsHaveTaxis implements FilterStrategy{
            @Override @Nonnull
            public boolean execute (Move move, AIGameState gameState){
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());
                ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph =
                        gameState.getSetup().graph;
                Set<EndpointPair<Integer>> edges =
                        gameState.getSetup().graph.incidentEdges(move.source());

                // if it isn't (both a secret ticket and all the edges use taxi tickets)
                return !(tickets.contains(ScotlandYard.Ticket.SECRET) &&
                        (edges
                        .stream()
                        .parallel()
                        .allMatch(e -> {
                            Optional<ImmutableSet<ScotlandYard.Transport>> optionalTransports = graph.edgeValue(e);
                            if (optionalTransports.isEmpty())
                                throw new IllegalArgumentException("Cannot find edge on graph");
                            return (optionalTransports.get().contains(ScotlandYard.Transport.TAXI));
                        })));
            }
        }

        private List<FilterStrategy> filterStrategies = ImmutableList.of(
                new RemoveFromFirstTwoRounds(),
                new RemoveFromRevealingRound(),
                new AllPossibleLocationsHaveTaxis()
        );

        public List<FilterStrategy> getFilterStrategies() {
            return filterStrategies;
        }

        /**
         *
         * @param move Move to be checked
         * @param gameState gamestate to pass in any required information for the filter algorithms
         * @return boolean if move satisfies all filtering
         * */
        public boolean checkMove (Move move, AIGameState gameState) {
            if (move.commencedBy().isMrX()){
//              If a move satisfies all filter algorithms then returns true

                return filterStrategies
                        .stream()
                        .allMatch(f -> f.execute(move, gameState));
            }
            // If the move commenced is done by a detective.
            return true;
        }
    }


    /**
     * Categorize locations based on distance (max, avg) from detective and types of connections
     * a location has
     * */
    class LocalizationCategorization {
        //TODO: I have designed a framework for Localization Categorization. We need to implement the return types
        //and the actual implementation.

        private void categorizeMinimumDistance(){

        }

        private void categorizeAverageDistance(){

        }

        private void categorizeStationType(){

        }

        //Main method to apply all categorization techniques
        public void applyCategories(){

        }

    }

    /**
     * Epsilon greedy playout
     * */
    class EGreedyPlayouts {
        //TODO: I have designed a framework for E-Greedy playouts.

        //For Detectives
        public Move getDetectiveBestMove (
                ImmutableSet<Move> moves,
                AIGameState gameState,
                PossibleLocations possibleLocations ){
            final Table<Integer, Integer, Integer> distances = DistancesSingleton.getInstance().getDistances();
            final ImmutableSet<Integer> locations = possibleLocations.getLocations();

            int minimumDistance = Integer.MAX_VALUE;
            Move bestMove = moves.asList().get(0);
            MoveVisitors.DestinationVisitor destinationVisitor = new MoveVisitors.DestinationVisitor();

            for (Move move : moves) {
                int destination = move.accept(destinationVisitor);

//              Not using stream chain due to worse performance.
                int sumDistance = 0;
                for (int l : locations) {
                    sumDistance += distances.get(l, destination);
                }

                if (sumDistance < minimumDistance) {
                    minimumDistance = sumDistance;
                    bestMove = move;
                }
            }

            return bestMove;
        }

        //For MrX
        public Move getMrXBestMove(ImmutableSet<Move> moves, AIGameState gameState) {
            Table<Integer, Integer, Integer> distances = DistancesSingleton.getInstance().getDistances();
            List<Integer> detectiveLocations = gameState.getDetectiveLocations();

            int maximinDistance = Integer.MIN_VALUE;
            Move bestMove = moves.asList().get(0);
            for (Move move: moves) {
                int destination = move.accept(new MoveVisitors.DestinationVisitor());
                int minDistance = detectiveLocations
                        .stream()
                        .map(l -> distances.get(l, destination))
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElseThrow();
                if (minDistance > maximinDistance) {
                    maximinDistance = minDistance;
                    bestMove = move;
                }
            }

            return bestMove;
        }
    }

    /**
     * Set a value of r to ensure optimization between co-operation and independent
     * hunting of MR X
     * */
    class CoalitionReduction {

        private final double r = 0.4;
        /**
         * Application of Coalition Reduction. If root piece is detective but not the value piece then
         * give only (1-r) times the weighting on the value.
         *
         * @param currentPiece the root piece as used in the node data structure
         * @param value the piece to compare against the root piece
         * @returns a double giving the evaluation score for that win
         * */
        public double calculateValue (Piece currentPiece, Piece value) {
            //if MrX or a matching detective piece
            if (currentPiece.equals(value)) return 1;
            else if (currentPiece.isDetective() && value.isDetective()) {
                return (1 - this.r);
            }
            else return 0;
        }
    }

    class ExplorationCoefficient {
        public double getMrXCoefficient () {
            return 0.2;
        }

        public double getDetectiveCoefficient () {
            return 2.0;
        }
    }

}
