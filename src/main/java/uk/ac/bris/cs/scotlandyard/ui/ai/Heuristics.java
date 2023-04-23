package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

        //Application of the strategy pattern
        public interface FilterStrategy {
            /**
             * Used to filter out moves. True keeps a move and false removes a move.
             *
             * @param move      Move to check
             * @param gameState Game state used for context
             * @return Boolean of whether move should be kept
             */
            boolean execute(Move move, AIGameState gameState);
        }

        /**
         * Filter any secret tickets used in the first two rounds of the game.
         * */
        public static class RemoveFromFirstTwoRounds implements FilterStrategy {
            @Override
            public boolean execute(Move move, AIGameState gameState) {
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

                boolean disallowed = false;
                for (int i = 0; i < tickets.size(); i++) {
                    if (tickets.get(i).equals(ScotlandYard.Ticket.SECRET) &&
                            gameState.getMrXTravelLog().size() + (i + 1) < 3) {
                        disallowed = true;
                    }
                }

                return !disallowed;
            }
        }

        /**
         * Filter any secret tickets that are used in the rounds where MrX are revealed
         * (Normally 3, 8, 13, 18, 24)
         * */
        public static class RemoveFromRevealingRound implements FilterStrategy {
            @Override
            public boolean execute(Move move, AIGameState gameState) {
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

                boolean disallowed = false;
                for (int i = 0; i < tickets.size(); i++) {

                    //first part: check if item is a secret ticket
                    //second part: check if it's a revealing round (+i to block the double move if move overlaps)
                    if (tickets.get(i).equals(ScotlandYard.Ticket.SECRET)
                            && gameState.getSetup().moves.get(gameState.getMrXTravelLog().size() + i)) {
                        disallowed = true;
                    }
                }

//              Check if move at current turn is a reveal move
                return !disallowed;
            }
        }

        /**
         * Filter any secret tickets where all possible destinations can be accessed with a taxi ticket instead
         * (Prevents wasteful usage of secret tickets)
         * */
        @SuppressWarnings("UnstableApiUsage")
        public static class AllPossibleLocationsHaveTaxis implements FilterStrategy {
            @Override
            public boolean execute(Move move, AIGameState gameState) {
                List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());
                ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph =
                        gameState.getSetup().graph;

                //Decompose a potential double move into two single moves if needed.
                List<Move.SingleMove> singleMoves = move.accept(new MoveVisitors.SingleMoveVisitor());

                boolean disallowed = false;
                for (int i = 0; i < singleMoves.size(); i++) {
                    boolean isSecret = tickets.get(i).equals(ScotlandYard.Ticket.SECRET);
                    int singleTicketLocation = (singleMoves.get(i)).source();

                    Set<EndpointPair<Integer>> edges =
                            gameState.getSetup().graph.incidentEdges(singleTicketLocation);

                    boolean allPossibleLocationsHaveTaxis = edges
                            .stream()
                            .parallel()
                            .allMatch(e -> graph
                                    .edgeValue(e)
                                    .orElseThrow()
                                    .contains(ScotlandYard.Transport.TAXI)
                            );

                    if (isSecret && allPossibleLocationsHaveTaxis) disallowed = true;
                }

                return !disallowed;
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
             * Checks a move against all three filter strategies
             * @param move      Move to be checked
             * @param gameState game state to pass in any required information for the filter algorithms
             * @return boolean if move satisfies all filtering
             */
            public boolean checkMove(Move move, AIGameState gameState) {
                if (move.commencedBy().isMrX()) {
//              If a move satisfies all filter algorithms then returns true

                    return filterStrategies
                            .stream()
                            .allMatch(f -> f.execute(move, gameState));
                }

                // If the move commenced is done by a detective. (no need to filter)
                return true;
            }
    }

        /**
         * Epsilon greedy playout.
         * Uses domain knowledge to make move choices in playouts more realistic.
         */
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
             */
            public Move getDetectiveBestMove(
                    ImmutableSet<Move> moves,
                    PossibleLocations possibleLocations) {
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
             */
            public Move getMrXBestMove(ImmutableSet<Move> moves, AIGameState gameState) {
                DistancesSingleton distances = DistancesSingleton.getInstance();
                List<Integer> detectiveLocations = gameState.getDetectiveLocations();

                //Assume closest distance (0)
                int maximinDistance = 0;
                Move bestMove = moves.asList().get(0);
                for (Move move : moves) {
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
         */
        class CoalitionReduction {

            private final double r = 0.375;

            public double getR() {
                return this.r;
            }

            /**
             * Application of Coalition Reduction. If root piece is detective but not the value piece then
             * give only (1-r) times the weighting on the value.
             *
             * @param currentPiece the root piece as used in the node data structure
             * @param value        the piece to compare against the root piece
             * @return a double giving the evaluation score for that win
             */
            public double calculateValue(Piece currentPiece, Piece value) {
                //if MrX or a matching detective piece
                if (currentPiece.equals(value)) return 1;
                else if (currentPiece.isDetective() && value.isDetective()) {
                    return (1 - this.r);
                } else return 0;
            }
        }

        class ExplorationCoefficient {
            public double getMrXCoefficient() {
                return 0.2;
            }

            public double getDetectiveCoefficient() {
                return 2.0;
            }
        }

         interface LocationCategorization {
             @SuppressWarnings("UnstableApiUsage")
             class MinDistanceData {
                 private final ImmutableMap<MinDistance, Category> data;

                 private MinDistanceData(ImmutableMap<MinDistance, Category> data) {
                     this.data = data;
                 }

                 static public MinDistanceData buildInitial () {
                     //Create a builder instance for immutable maps
                     ImmutableMap.Builder<MinDistance, Category> builder = ImmutableMap.builder();
                     Arrays.stream(MinDistance.values()).forEach(o -> builder.put(o, new Category()));
                     return new MinDistanceData(builder.build());
                 }

                 /**
                  * Extract minimum distance data from a file
                  * @param file file to be used as the data set
                  * @return {@link MinDistanceData} with data from file.
                  * @throws IOException if it is unable to read the file given or file in incorrect format.
                  * */
                 static public MinDistanceData buildFromContinuedFile (File file) throws IOException {
                     ImmutableMap.Builder<MinDistance, Category> builder = ImmutableMap.builder();

//                   Buffers the characters in the file for efficient reading
                     BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

//                   Skip header
                     bufferedReader.readLine();

                     String input = bufferedReader.readLine();
                     while(input != null) {
//                      Splits the string every comma
                        String[] fields = input.split(",");

//                       Must have three arguments: Category, total hits, and total possible
                         if (fields.length != 3) throw new IOException("File in invalid format");
                         builder.put(
                                 MinDistance.valueOf(fields[0]),

//                               Construct a category object with given total hits and total possible
                                 new Category(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]))
                         );

                         input = bufferedReader.readLine();
                     }

                     return new MinDistanceData(builder.build());
                 }

                 /**
                  * Extract minimum distance data from file in resources
                  * @return {@link MinDistanceData} with data from file.
                  * @throws IOException if it is unable to read the file given or file in incorrect format.
                  * */
                 static public MinDistanceData buildFromResources () throws IOException  {
                     ImmutableMap.Builder<MinDistance, Category> builder = ImmutableMap.builder();

                     String input = Resources.toString(
                         Resources.getResource("min-distance-data.txt"),
                         StandardCharsets.UTF_8
                    );

//                   Splits on any new lines.
                     String[] lines = input.split("\\R");

                     for (int i = 1; i < lines.length; i++) {
                         String[] fields = lines[i].split(",");
                         if (fields.length != 3) throw new IOException("File not in correct format.");
                         try {
                             builder.put(
                                 MinDistance.valueOf(fields[0]),
                                 new Category(Integer.parseInt(fields[1]), Integer.parseInt(fields[2]))
                            );
                         } catch (NumberFormatException e) {
                             throw new IOException("File not in correct format.");
                         }
                     }

                     return new MinDistanceData(builder.build());
                 }

                 /**
                  * @param category Category to get total hits for
                  * @return Total hits for category
                  */
                 public int getTotalHits(MinDistance category) {
                     return this.data.get(category).getTotalHits();
                 }

                 /**
                  * @param category Category to get total possible for
                  * @return Total possible for category
                  */
                 public int getTotalPossible(MinDistance category) {
                     return this.data.get(category).getTotalPossible();
                 }

                 /**
                  * Get probability that possible location is actual location for category
                  * @param category Category to get hit probability for.
                  * @return Hit probability for category.
                  */
                 public double getHitProbability (MinDistance category) {
                     return ((double) this.getTotalHits(category) / this.getTotalPossible(category));
                 }

                 /**
                  * Adds a hit for a category.
                  * @param category Category to add hit for
                  */
                 public void addHit(MinDistance category) {
                     this.data.get(category).addHit();
                 }

                 /**
                  *  Adds a miss for a category
                  * @param  category Category to add a miss for
                  *  */
                 public void addMiss(MinDistance category) {
                     this.data.get(category).addMiss();
                 }

                 /**
                  * Specific category for a classification.
                  */
                 static private class Category {

                     //Total correct locations of Mr X
                     private int totalHits;

                     //Total number of times it was possible for it to be a location of Mr X
                     private int totalPossible;

                     //Default constructor
                     public Category() {
                         this.totalHits = 0;
                         this.totalPossible = 0;
                     }

                     public Category(int totalHits, int totalPossible) {
                         this.totalHits = totalHits;
                         this.totalPossible = totalPossible;
                     }

                     public int getTotalHits() {
                         return this.totalHits;
                     }

                     public int getTotalPossible() {
                         return this.totalPossible;
                     }

                     public void addHit() {
                         this.totalHits++;
                     }

                     public void addMiss() {
                         this.totalPossible++;
                     }
                 }
             }

             /**
              * Minimum distance from a detective to Mr X.
              * Categorized by distance in turns
              * */
             enum MinDistance {
                 ONE,
                 TWO,
                 THREE,
                 FOUR,
                 FIVE_PLUS;

                 /**
                  * Given an int return the category.
                  * @throws IllegalArgumentException if int invalid distance (less than or equal to 0)
                  * */
                 public static MinDistance getCategoryFromDistance(int distance) {
                     if (distance <= 0) throw new IllegalArgumentException("Distance must be > 0");

                     switch (distance) {
                         case 1 -> {return ONE;}
                         case 2 -> {return TWO;}
                         case 3 -> {return THREE;}
                         case 4 -> {return FOUR;}
                         default -> {return FIVE_PLUS;}
                     }
                 }
             }
         }
}
