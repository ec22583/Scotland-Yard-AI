package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Player;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DetectiveAI implements AI{
    private PossibleLocations possibleLocations;
    final private AIGameStateFactory aiGameStateFactory;
    final private PossibleLocationsFactory possibleLocationsFactory;
    final private DistancesSingleton distances;
    private Node mctsTree;

    public DetectiveAI (DistancesSingleton distances) {
        this.distances = distances;
        this.aiGameStateFactory = new AIGameStateFactory();
        this.possibleLocationsFactory = new PossibleLocationsFactory();
    }

    //Helper function of generateBestMove
    private void sleepThread(Pair<Long, TimeUnit> timeoutPair){
        try {
//          Sleeps the program for the (time - 250 ms).
            Thread.sleep(TimeUnit.MILLISECONDS.convert(timeoutPair.left(), timeoutPair.right()) - 250);
        } catch (InterruptedException e) {
            // Handles if an interrupt is thrown towards the current method while asleep.
//            Thread.currentThread().interrupt();
        }
    }

    //Runs MCTS and AI Threads
    public void runThreads(Pair<Long, TimeUnit> timeoutPair){
        Thread m = Thread.currentThread();
        MCTS mcts = new MCTS(mctsTree, m);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();
        this.sleepThread(timeoutPair); //Sleeps program to let MCTS algorithm run

        if (mcts.isAlive()) mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.
    }

    @Override
    public Move generateBestMove(Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
        }
        this.possibleLocations = this.possibleLocations.updateLocations(board);

        System.out.printf("Current locations: %s%n", this.possibleLocations.getLocations());

        List<Pair<AIGameState, Integer>> gameStates = aiGameStateFactory.buildDetectiveGameStates(board, this.possibleLocations);
//      Remove any already winning game states since they are not possible.
        gameStates = gameStates.stream().filter(s -> s.left().getWinner().isEmpty()).toList();

//        Pair<AIGameState, Integer> possibleGameState = null;
//        int maximinDistance = Integer.MIN_VALUE;
//        for (Pair<AIGameState, Integer> gameStatePair : gameStates) {
//            List<Integer> detectiveLocations = gameStatePair.left().getDetectiveLocations();
//            int currentMinimumDistance = detectiveLocations
//                    .stream()
//                    .map(l -> distances.get(l, gameStatePair.right()))
//                    .mapToInt(Integer::intValue)
//                    .min()
//                    .orElseThrow();
//
//             if (currentMinimumDistance > maximinDistance) {
//                 maximinDistance = currentMinimumDistance;
//                 possibleGameState = gameStatePair;
//             }
//        }

        AIGameState randomGameState = gameStates.get(
                new Random().nextInt(gameStates.size())
        ).left();

        //create a Node with all heuristics fed in
        this.mctsTree = new Node(
                randomGameState,
                this.possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient(),
                new Heuristics.EGreedyPlayouts()
        );

        runThreads(timeoutPair);

        return this.mctsTree.getBestChild().getPreviousMove();
    }
}
