package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.Optional;

// MCTS Algorithm (Monte Carlo tree search)
public class MCTS extends Thread {

    private Thread parentThread;
    final private Node mctsTree;
    final Heuristics.EGreedyPlayouts eGreedyPlayouts;

    public MCTS (Node mctsTree, Thread parentThread) {
        this.mctsTree = mctsTree;
        this.parentThread = parentThread;
        this.eGreedyPlayouts = new Heuristics.EGreedyPlayouts();
    }

    /**
     * @throws IllegalStateException Can't get winning piece of a game state
     * */
    public void iterationAlgorithm () {
        Node node = this.mctsTree;
        Piece gameValue;

        // Selection Stage.
        // Stops selecting when node is not fully expanded or game is already won.
        while (node.isFullyExpanded() && node.isNotGameOver()) {
            node = node.selectChild();
        }

//      If game state is not a winning game state.
        // Else don't run expansion simulation or backpropagation
        if (node.isNotGameOver()) {
            // Expansion Stage
            node = node.expandNode();

            // Simulation Stage
            gameValue = Node.simulateGame(
                    node.getGameState(),
                    node.getPossibleLocations(),
                    this.eGreedyPlayouts
            );
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

        //Cap numIterations at 25000 because there is no noticeable behavioural improvements after this point
        //Additionally it is also to make AI take their turn faster towards the end of the game
        while(!Thread.interrupted() && (numIterations < 25000)) {
            numIterations++;
            this.iterationAlgorithm();
        }
        System.out.println("Number of iterations: " + numIterations);

        if (numIterations >= 25000) {
            this.parentThread.interrupt();
        }
    }

}
