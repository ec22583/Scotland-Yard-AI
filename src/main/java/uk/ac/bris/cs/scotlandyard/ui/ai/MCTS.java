package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.Optional;

/**
 * MCTS (Monte Carlo Tree Search) Algorithm
 *
 * Formatting of the steps of the algorithm slightly based on:
 * <a href="https://www.youtube.com/watch?v=wuSQpLinRB4">https://www.youtube.com/watch?v=wuSQpLinRB4</a>
 */
public class MCTS extends Thread {
    final private Node mctsTree;
    final Heuristics.EGreedyPlayouts eGreedyPlayouts;

    /**
     * @param mctsTree the mcts tree to apply the algorithm to
     * */
    public MCTS (Node mctsTree) {
        this.mctsTree = mctsTree;
        this.eGreedyPlayouts = new Heuristics.EGreedyPlayouts();
    }

    /**
     * @throws IllegalStateException Can't get winning piece of a game state
     * */
    public void iterationAlgorithm () {
        Pair<Node, Boolean> nodeBooleanPair;
        Node node = this.mctsTree;
        Piece gameValue;
//      Stores whether latest child was from selection or expansion.
        boolean selected = true;

        // Selection Stage.
        // Stops selecting when node is not fully expanded or game is already won.
        while (selected && node.isNotGameOver()) {
            nodeBooleanPair = node.expandOrSelect();
            node = nodeBooleanPair.left();
            selected = nodeBooleanPair.right();
        }

//      If game state is not a winning game state.
        // Else don't run simulation or backpropagation
        if (node.isNotGameOver()) {
            // Simulation Stage
            gameValue = Node.simulateGame(
                    node.getGameState(),
                    node.getPossibleLocations(),
                    this.eGreedyPlayouts
            );
        }
        else {
            gameValue = Node.getGameWinner(node.getGameState()).orElseThrow();
        }

        // Backpropagation Stage
        node.backPropagation(gameValue);
    }

    //Main component to execute the algorithm
    @Override
    public void run () {
        //Cap numIterations at 30000 because there is no noticeable behavioural improvements after this point
        //Additionally it is also to make AI take their turn faster towards the end of the game
        this.iterationAlgorithm();
    }

}
