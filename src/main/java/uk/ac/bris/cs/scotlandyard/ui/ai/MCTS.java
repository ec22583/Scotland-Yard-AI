package uk.ac.bris.cs.scotlandyard.ui.ai;

// MCTS Algorithm (Monte Carlo tree search)
public class MCTS extends Thread {
    private Node mctsTree = null;

    public MCTS (Node mctsTree) {
        this.mctsTree = mctsTree;
    }


    public void iterationAlgorithm () {
        Node node = this.mctsTree;

        // Selection Stage
        while (node.isFullyExpanded()) {
            node = node.selectChild();
        }

        Node.GameValue gameValue;

//      If game state is not a winning game state.
        // Else don't run expansion simulation or backpropagation
        if (!node.isGameOver()) {
            // Expansion Stage
            node = node.expandNode();

            // Simulation Stage
            gameValue = node.simulateGame();
        } else {
            gameValue = Node.getGameWinner(node.getGameState());
        }

        // Backpropagation Stage
        node.backPropagation(gameValue);
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
