package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// MCTS Algorithm (Monte Carlo tree search)
public class MCTS extends Thread {
    private Tree<TreeGameState> gameStateTree;

    public MCTS (Tree<TreeGameState> tree) {
        this.gameStateTree = tree;

    }

    /**
     * @param c constant to balance exploration and exploitation
     * @param np number of visits of parent (current) node
     * @param ni number of visits of child node
     * @param wi number of wins  for child node
     */
    static double calculateUCB1 (double c, double np, double ni, double wi) {
        double avgScore = wi/ni;
        double explorationFactor = c * Math.sqrt(Math.log(np)/ni);
        return avgScore + explorationFactor;
    }

//  Recursively plays a turn (from available moves from currentNode's game state) and updates the tree.
//  Once game is finished, adds the win or loss statistics up the tree recursively.
    private boolean playTurn (Tree<TreeGameState> currentNode){
        List<Move> availableMoves = currentNode.getValue().getGameState().getAvailableMoves().asList();
        List<TreeGameState> childValues = currentNode.getChildValues();

        Move nextMove = null;
//      Not all moves from this game state have been searched yet.
        if (currentNode.getChildNodes().size() < availableMoves.size()) {
//          Filters out all moves that have already been visited.
            availableMoves.removeAll(
                    childValues
                            .stream()
                            .map(t -> t.getPreviousMove())
                            .toList()
            );
        }
//      All moves have been visited at least once, so we can use the UCT selection strategy.
        else {
            Move bestMove = childValues.get(0).getPreviousMove();
            Board.GameState bestGameState = childValues.get(0).getGameState();
            double bestScore = MCTS.calculateUCB1(
                    0.8,
                    currentNode.getValue().getTotalPlays(),
                    childValues.get(0).getTotalPlays(),
                    childValues.get(0).getWins()
            );
            childValues.remove(0);
            for (TreeGameState childValue : childValues) {
                double currentChildScore = MCTS.calculateUCB1(
                        0.8,
                        currentNode.getValue().getTotalPlays(),
                        childValue.getTotalPlays(),
                        childValue.getWins()
                );
//                if (c)
            }
        }

        Move randomMove = availableMoves.get(new Random().nextInt(availableMoves.size()));

        Board.GameState newGameState = currentNode.getValue().getGameState().advance(randomMove);
        TreeGameState newTreeGameState = new TreeGameState(newGameState, randomMove);
        Optional<Tree<TreeGameState>> optionalChild =
                this.gameStateTree.getChildNodeEqualling(newTreeGameState);

        Tree<TreeGameState> newTreeNode;
        if (optionalChild.isEmpty()) {
//          Returns the new Tree node created for value.
            newTreeNode = currentNode.addChildValue(newTreeGameState);
        }
        else newTreeNode = optionalChild.get(); //child Contains a value

        boolean win;
        if (!newTreeNode.getValue().getGameState().getWinner().isEmpty()) {
//          Anchor case. Occurs once game has finished.
            win = newGameState.getWinner().contains(MRX);
        }
        else {
//          Recursive case. Traverse to the new tree node in the Tree<GameStateTree> (Plays another turn)
            win = this.playTurn(newTreeNode);
        }

        if (win) currentNode.getValue().addWin();
        else currentNode.getValue().addLoss();

        return win;
    }

    //Main component to execute the algorithm
    @Override
    public void run () {
        while(!Thread.interrupted()) {
            this.playTurn(this.gameStateTree);
        }
    }

}
