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

public class DetectiveAI implements PlayerAI {
    private static final long BUFFER = 200;
    private PossibleLocations possibleLocations;
    final private AIGameStateFactory aiGameStateFactory;
    final private PossibleLocationsFactory possibleLocationsFactory;
    final private DistancesSingleton distances;
    private Heuristics.LocationCategorization.MinDistanceData minDistanceData;

    /**
     * @param distances Table of precalculated distances for graph.
     *  */
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

    /**
     * Filter out winning game states
     * @param gameStates gamestates to be inspected as winning game states
     * */
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

    /**
     * Given an already filtered list of non-winning game states, use the distances to possible Mr X and detectives to
     * generate a weighting.
     * @param gameStates Pairs of game states and their corresponding possible location.
     * @param detectiveLocations Detective locations used to find distances to Mr X.
     * @return list of pairs of AIGameState and the Double representing their weightings.
     * */
    private List<Pair<AIGameState, Double>> createWeightedGameStates(List<Pair<AIGameState, Integer>> gameStates,
                                                                     List<Integer> detectiveLocations){

        return gameStates
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
    }

    @Override @Nonnull
    public Move generateBestMove(Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
        }
        this.possibleLocations = this.possibleLocations.updateLocations(board);

//      Gets all possible game states for turn.
        List<Pair<AIGameState, Integer>> gameStates = aiGameStateFactory.buildDetectiveGameStates(board, this.possibleLocations);

        List<Integer> detectiveLocations = gameStates.get(0).left().getDetectiveLocations();

//      Remove any already winning game states since they are not possible.
        gameStates = removeWinningGamestates(gameStates);

//      Pairs of game states and their weightings for random selection.
        List<Pair<AIGameState, Double>> weightedGameStates = createWeightedGameStates(gameStates, detectiveLocations);

        double totalWeight = weightedGameStates
                .stream()
                .map(Pair::right)
                .mapToDouble(Double::doubleValue)
                .sum();

//      Generates a random double from 0 to totalWeight exclusive
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

        return PlayerAI.runMCTSForGameState(gameState, possibleLocations, timeoutPair, BUFFER);
    }
}
