package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import org.checkerframework.checker.nullness.qual.NonNull;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

//Wrapper class for all Heuristics (classes)
public interface Heuristics {


    /**
     * Apply rules to improve MrX's use of secret tickets (Ticket economy)
     * */
    class MoveFiltering {

        //Variables used exclusively in unit testing
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

        public static class RemoveFromFirstTwoRounds implements FilterStrategy{
            @Override @NonNull
            public boolean execute (Move move, AIGameState gameState){
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

                return !tickets.contains(ScotlandYard.Ticket.SECRET) ||
                        gameState.getMrXTravelLog().size() > 2;
            }
        }

        public static class RemoveFromRevealingRound implements FilterStrategy{
            @Override @Nonnull
            public boolean execute (Move move, AIGameState gameState){
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

                boolean disallowed = false;
                for (int i = 0; i < tickets.size(); i++) {

                    //first part: check if item is a secret ticket
                    //second part: check if it's a revaling round (+i to block the double move if move overlaps)
                    if (tickets.get(i).equals(ScotlandYard.Ticket.SECRET)
                    && gameState.getSetup().moves.get(gameState.getMrXTravelLog().size() + i) == true) {
                        disallowed = true;
                    }
                }

//              Check if move at current turn is a reveal move
                return !disallowed;
            }
        }

        public static class AllPossibleLocationsHaveTaxis implements FilterStrategy{
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
                        .allMatch(e -> graph
                                .edgeValue(e)
                                .orElseThrow()
                                .contains(ScotlandYard.Transport.TAXI)
                        )));
            }
        }

        final private List<FilterStrategy> filterStrategies = ImmutableList.of(
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
         * @param gameState game state to pass in any required information for the filter algorithms
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
     * Epsilon greedy playout.
     * Uses domain knowledge to make move choices in playouts more realistic.
     * */
    class EGreedyPlayouts {
        final public double EPSILON = 0.2;
        final private DistancesSingleton distances;

        public EGreedyPlayouts() {
            this.distances = DistancesSingleton.getInstance();
        }

        /**
         * Heuristic that minimizes the distance to MrX.
         * Select the move that minimizes the sum of the distances to all possible
         * locations to MrX. If there are multiple minimum solution, choose the most
         * recently inspected best Move
         * */
        public Move getDetectiveBestMove (
                ImmutableSet<Move> moves,
                PossibleLocations possibleLocations ){
            ImmutableSet<Integer> locations = possibleLocations.getLocations();

            //Assume max distance
            int minimumDistance = Integer.MAX_VALUE;
            Move bestMove = moves.asList().get(0);
            MoveVisitors.DestinationVisitor destinationVisitor = new MoveVisitors.DestinationVisitor();

            for (Move move : moves) {
                int destination = move.accept(destinationVisitor);
                int sumDistance = 0;

//              Not using stream chain due to worse performance.
                for (int location : locations) {
                    sumDistance += distances.get(location, destination);
                }
                if (sumDistance < minimumDistance) {
                    minimumDistance = sumDistance;
                    bestMove = move;
                }
            }

            return bestMove;
        }

         /**
         * Heuristic that maximises the distances to detectives.
         * Select the move that maximises the sum of the distances to all possible
         * locations to detectives. If there are multiple minimum solution, choose the most
         * recently inspected best Move
         * */
        public Move getMrXBestMove(ImmutableSet<Move> moves, AIGameState gameState) {
            DistancesSingleton distances = DistancesSingleton.getInstance();
            List<Integer> detectiveLocations = gameState.getDetectiveLocations();

            //Assume closest distance (0)
            int maximinDistance = 0;
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

        private final double r = 0.375;

        public double getR(){
            return this.r;
        }

        /**
         * Application of Coalition Reduction. If root piece is detective but not the value piece then
         * give only (1-r) times the weighting on the value.
         *
         * @param currentPiece the root piece as used in the node data structure
         * @param value the piece to compare against the root piece
         * @return a double giving the evaluation score for that win
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
