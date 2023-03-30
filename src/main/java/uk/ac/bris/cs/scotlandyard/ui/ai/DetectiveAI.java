package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DetectiveAI implements AI{
    private PossibleLocations possibleLocations;
    final private AIGameStateFactory aiGameStateFactory;
    final private PossibleLocationsFactory possibleLocationsFactory;
    private Node mctsTree;

    public DetectiveAI () {
        this.aiGameStateFactory = new AIGameStateFactory();
        this.possibleLocationsFactory = new PossibleLocationsFactory();
    }

    //Helper function of generateBestMove
    private void sleepThread(Pair<Long, TimeUnit> timeoutPair){
        try {
//          Sleeps the program for the (time - 500 ms).
            Thread.sleep(TimeUnit.MILLISECONDS.convert(timeoutPair.left(), timeoutPair.right()) - 500);
        } catch (InterruptedException e) {
            // Handles if an interrupt is thrown towards the current method while asleep.
            Thread.currentThread().interrupt();
        }
    }

    //Runs MCTS and AI Threads
    public void runThreads(Pair<Long, TimeUnit> timeoutPair){
        MCTS mcts = new MCTS(mctsTree);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();
        this.sleepThread(timeoutPair); //Sleeps program to let MCTS algorithm run
        mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.
    }

    @Override
    public Move generateBestMove(Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildFromInitialBoard(board);
        }
        //If the turn count is less than the travel log size
        else if (this.possibleLocations.getTurn() < board.getMrXTravelLog().size()) {
            if (board.getMrXTravelLog().get(board.getMrXTravelLog().size() - 1).location().isPresent()) {
                this.possibleLocations =
                        this.possibleLocations.newKnownLocation(board, board.getMrXTravelLog().size() - 1);
            }
            // if it is at a revealing move
            else if (board.getMrXTravelLog().size() > 1 && board.getMrXTravelLog().get(board.getMrXTravelLog().size() - 2).location().isPresent()) {
                this.possibleLocations =
                        this.possibleLocations.newKnownLocation(board, board.getMrXTravelLog().size() - 2);
                this.possibleLocations = this.possibleLocations.updateLocations(board);
            } else {
                this.possibleLocations = this.possibleLocations.updateLocations(board);
            }
        }

        System.out.printf("Current locations: %s%n", this.possibleLocations.getLocations());

        List<AIGameState> gameStates = aiGameStateFactory.buildDetectiveGameStates(board, this.possibleLocations);
//      Remove any already winning game states since they are not possible.
        gameStates = gameStates.stream().filter(s -> s.getWinner().isEmpty()).toList();

        AIGameState randomGameState = gameStates.get(
                new Random().nextInt(gameStates.size())
        );

        this.mctsTree = new Node(
                randomGameState,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient()
        );

        runThreads(timeoutPair);

        return this.mctsTree.getBestChild().getPreviousMove();
    }
}
