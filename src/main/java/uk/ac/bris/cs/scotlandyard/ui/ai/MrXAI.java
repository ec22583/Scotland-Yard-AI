package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.awt.*;
import java.sql.Time;
import java.util.*;
import java.lang.Thread;
import java.util.List;
import java.util.concurrent.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// Separate AI Entity Behaviour
public class MrXAI implements AI {
    private GameStateFactory gameStateFactory;
    Node mctsTree;
    Board.GameState gameState;

    public MrXAI () {
        this.gameStateFactory = new GameStateFactory();
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

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair) {
        this.gameState = gameStateFactory.generateMrXGameState(board);

        this.mctsTree = new Node(this.gameState);
        MCTS mcts = new MCTS(mctsTree);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();
        this.sleepThread(timeoutPair); //Sleeps program to let MCTS algorithm run
        mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.

        return this.mctsTree.getBestChild().getPreviousMove();
    }
}
