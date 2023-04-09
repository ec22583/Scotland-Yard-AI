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
    private AIGameStateFactory aiGameStateFactory;
    private PossibleLocations possibleLocations;
    private PossibleLocationsFactory possibleLocationsFactory;
    private Node mctsTree;

    public MrXAI () {
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

    public void runThreads(Pair<Long, TimeUnit> timeoutPair){

        Thread m = Thread.currentThread();
        MCTS mcts = new MCTS(mctsTree, m);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();
        this.sleepThread(timeoutPair); //Sleeps program to let MCTS algorithm run

        if (mcts.isAlive()) mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
        }
        this.possibleLocations = this.possibleLocations.updateLocations(board);

        AIGameState gameState = this.aiGameStateFactory.buildMrXGameState(board);

        this.mctsTree = new Node(
                gameState,
                this.possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient(),
                new Heuristics.EGreedyPlayouts()
        );

        this.runThreads(timeoutPair);

        return this.mctsTree.getBestChild().getPreviousMove();
    }
}
