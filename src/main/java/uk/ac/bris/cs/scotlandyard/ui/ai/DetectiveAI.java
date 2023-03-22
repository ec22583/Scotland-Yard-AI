package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DetectiveAI implements AI{
    private PossibleLocations possibleLocations;
    private GameStateFactory gameStateFactory;
    private PossibleLocationsFactory possibleLocationsFactory;
    private List<Board.GameState> gameStates;
    private Node mctsTree;

    public DetectiveAI () {
        this.gameStateFactory = new GameStateFactory();
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

    @Override
    public Move generateBestMove(Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildFromInitialBoard(board);
        } else {
            if (board.getMrXTravelLog().get(board.getMrXTravelLog().size() - 1).location().isPresent()) {
                this.possibleLocations = this.possibleLocations.newKnownLocation(board);
            } else {
                this.possibleLocations = this.possibleLocations.updateLocations(board);
            }
        }

        this.gameStates = gameStateFactory.generateDetectiveGameStates(board, this.possibleLocations);

        Board.GameState randomGameState = this.gameStates.get(
                new Random().nextInt(this.gameStates.size())
        );

        this.mctsTree = new Node(randomGameState);
        MCTS mcts = new MCTS(mctsTree);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();
        this.sleepThread(timeoutPair); //Sleeps program to let MCTS algorithm run
        mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.

        return this.mctsTree.getBestChild().getPreviousMove();
    }
}
