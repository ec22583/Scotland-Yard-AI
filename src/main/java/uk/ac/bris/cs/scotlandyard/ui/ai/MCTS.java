package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Piece;

/**
 * MCTS (Monte Carlo Tree Search) Algorithm
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

//  Main component to execute the algorithm
    @Override
    public void run () {
        Pair<Node, Boolean> nodeBooleanPair;
        Node node = this.mctsTree;
        Piece gameValue;
//      Stores whether latest child was from selection or expansion.
        boolean selected = true;

//      Selection Stage.
//      Stops selecting when node is not fully expanded or game is already won.
        while (selected && node.isNotGameOver()) {
            nodeBooleanPair = node.expandOrSelect();
            node = nodeBooleanPair.left();
            selected = nodeBooleanPair.right();
        }

//      Only run simulation if non-winning game state for node.
        if (node.isNotGameOver()) {
            gameValue = Node.simulateGame(
                    node.getGameState().orElseThrow(),
                    node.getPossibleLocations().orElseThrow(),
                    this.eGreedyPlayouts
            );
        }
        else {
            gameValue = Node.getGameWinner(node.getGameState().orElseThrow()).orElseThrow();
        }

//      Backpropagation Stage
        node.backPropagation(gameValue);
    }
}
