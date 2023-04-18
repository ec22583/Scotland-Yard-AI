package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DetectiveAI implements AI{
    private PossibleLocations possibleLocations;
    final private AIGameStateFactory aiGameStateFactory;
    final private PossibleLocationsFactory possibleLocationsFactory;
    final private DistancesSingleton distances;

    public DetectiveAI (DistancesSingleton distances) {
        this.distances = distances;
        this.aiGameStateFactory = new AIGameStateFactory();
        this.possibleLocationsFactory = new PossibleLocationsFactory();
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

        System.out.printf("Current locations: %s%n", this.possibleLocations.getLocations());

        List<Pair<AIGameState, Integer>> gameStates = aiGameStateFactory.buildDetectiveGameStates(board, this.possibleLocations);

//      Remove any already winning game states since they are not possible.
        gameStates = gameStates
                .stream()
                .filter(s ->
                        s.left()
                        .getWinner()
                        .isEmpty()
                )
                .toList();

        AIGameState randomGameState = gameStates.get(
                new Random().nextInt(gameStates.size())
        ).left();

        //create a Node with all heuristics fed in
        Node mctsTree = new Node(
                randomGameState,
                this.possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient()
        );

        AI.runThreads(mctsTree, timeoutPair);

        return mctsTree.getBestChild().getPreviousMove();
    }
}
