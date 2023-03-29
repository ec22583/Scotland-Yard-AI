package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.Optional;

// MCTS Algorithm (Monte Carlo tree search)
public class MCTS extends Thread {
    final private Node mctsTree;

    public MCTS (Node mctsTree) {
        this.mctsTree = mctsTree;
    }


    public void iterationAlgorithm () {
        Node node = this.mctsTree;

        // Selection Stage.
        // Stops selecting when node is not fully expanded or game is already won.
        while (node.isFullyExpanded() && node.isNotGameOver()) {
            node = node.selectChild();
        }

        Piece gameValue;

//      If game state is not a winning game state.
        // Else don't run expansion simulation or backpropagation
        if (node.isNotGameOver()) {
            // Expansion Stage
            node = node.expandNode();

            // Simulation Stage
            gameValue = node.simulateGame();
        }
        else {
            Optional<Piece> optionalPiece = Node.getGameWinner(node.getGameState());
            if (optionalPiece.isEmpty())
                throw new IllegalStateException("Cannot get winning piece of game state");

            gameValue = optionalPiece.get();
        }

        // Backpropagation Stage
        Piece value = node.backPropagation(gameValue);
    }

    //Main component to execute the algorithm
    @Override
    public void run () {
        int numIterations = 0;
        while(!Thread.interrupted()) {
            numIterations++;
            this.iterationAlgorithm();
        }
        System.out.println("Number of iterations: " + numIterations);
    }

}
