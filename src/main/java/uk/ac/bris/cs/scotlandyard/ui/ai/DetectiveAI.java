package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.ui.ai.Heuristics.LocationCategorization.MinDistance;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DetectiveAI implements AI{
    private PossibleLocations possibleLocations;
    final private AIGameStateFactory aiGameStateFactory;
    final private PossibleLocationsFactory possibleLocationsFactory;
    final private DistancesSingleton distances;
    private Heuristics.LocationCategorization.MinDistanceData minDistanceData;

    public DetectiveAI (DistancesSingleton distances) {
        this.distances = distances;
        this.aiGameStateFactory = new AIGameStateFactory();
        this.possibleLocationsFactory = new PossibleLocationsFactory();

        try {
            this.minDistanceData = Heuristics.LocationCategorization.MinDistanceData.buildFromResources();
        } catch (IOException e) {
            System.err.println("Cannot read min-distance-data.txt from resources.");
            System.exit(1);
        }

    }


    private List<Pair<AIGameState, Integer>> removeWinningGamestates(List<Pair<AIGameState, Integer>> gameStates){
        return gameStates
                .stream()
                .filter(s ->
                        s.left()
                        .getWinner()
                        .isEmpty()
                )
                .toList();
    }

    private List<Pair<AIGameState, Double>> createWeightedGameStates(List<Pair<AIGameState, Integer>> gamestates,
                                                                     List<Integer> detectiveLocations){

//        List<Pair<AIGameState, Double>> weightedGameStates= gamestates
//                .stream()
//                //Generate a Pair<AIGameState, MinDistance>
//                .map(p -> {
//                    return new Pair<>(
//                            p.left(),
//                            MinDistance.getCategoryFromDistance(
//                                detectiveLocations
//                                        .stream()
//                                        .mapToInt(l -> distances.get(l, p.right()))
//                                        .min()
//                                        .orElseThrow()
//                        )
//                    );
//                })
//                //Generate it again into a Pair<AiGameState, Double>
//                .map(p ->
//                        new Pair<>(p.left(), this.minDistanceData.getHitProbability(p.right())))
//                .toList();

        List<Pair<AIGameState, Double>> weightedGameStates = gamestates
                .stream()
                //Generate a Pair<AIGameState, Double>
                .map(p -> {
                    return new Pair<>(
                            p.left(),

                            this.minDistanceData.getHitProbability(
                                    //Feed in the detective locations and choose the minimum distance

                                    //This is type MinDistance
                                    MinDistance.getCategoryFromDistance(detectiveLocations
                                        .stream()
                                            //extract the distances between the detective location and the
                                            //possible location of Mr X
                                        .mapToInt(l -> distances.get(l, p.right()))
                                        .min()
                                        .orElseThrow())
                        )
                    );
                })
                .toList();

        return weightedGameStates;
    }

    /**
     * Given the game state, generate the best move for Detective
     * @param board game state in which to evaluate the best move
     * @param timeoutPair amount of time the thread lasts for
     * */
    @Override @Nonnull
    public Move generateBestMove(Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
        }
        this.possibleLocations = this.possibleLocations.updateLocations(board);

        List<Pair<AIGameState, Integer>> gameStates = aiGameStateFactory.buildDetectiveGameStates(board, this.possibleLocations);
        List<Integer> detectiveLocations = gameStates.get(0).left().getDetectiveLocations();

//      Remove any already winning game states since they are not possible.
        gameStates = removeWinningGamestates(gameStates);

        List<Pair<AIGameState, Double>> weightedGameStates = createWeightedGameStates(gameStates, detectiveLocations);

        double totalWeight = weightedGameStates
                .stream()
                .map(Pair::right)
                .mapToDouble(Double::doubleValue)
                .sum();

        // extract a random double from the total weight
        double randomDouble = new Random().nextDouble(totalWeight);

        AIGameState gameState = weightedGameStates.get(0).left();
        double runningTotal = 0.0;
        int i = 0;

        // chooses a game state based on the weights of the categorization
        while (runningTotal < randomDouble){
            //Extract the game state from the Pair data type
            gameState = weightedGameStates.get(i).left();
            runningTotal += weightedGameStates.get(i).right();
            i++;
        }

        //create a Node with all heuristics fed in
        Node mctsTree = new Node(
                gameState,
                this.possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient()
        );

        AI.runThreads(mctsTree, timeoutPair);

        return mctsTree.getBestChild().getPreviousMove();
    }
}
