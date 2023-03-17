package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
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
     * @param c constant to balance exploration and exploitation (Higher for more exploration)
     * @param np number of visits of parent (current) node
     * @param ni number of visits of child node
     * @param wi number of wins for child node
     */
    static double calculateUCB1 (double c, double np, double ni, double wi) {
        double avgScore = wi/ni;
        double explorationFactor = c * Math.sqrt(Math.log(np)/ni);
        return avgScore + explorationFactor;
    }

    //Helper to calculateUCB1. Used to process parent and child into appropriate values and types
    static double processForUCB1 (double c, Tree<TreeGameState> parent ,TreeGameState child){
        double np = parent.getValue().getTotalPlays();
        double ni = child.getTotalPlays();
        double wi = child.getWins();

        return calculateUCB1(c, np, ni, wi);
    }

    //Helper function to playTurn
    /**
     @param moves List of yet to be filtered moves
     @param childValues List<TreeGameState> -> List<Moves> in which used to remove from moves
     */
    private List<Move> removeMovesContaining (List<Move> moves, List<TreeGameState> childValues){
        moves.removeAll(
                childValues
                        .stream()
                        .parallel()
                        .map(t -> t.getPreviousMove())
                        .toList()
                );

        return moves;
    }

//  Recursively plays a turn (from available moves from currentNode's game state) and updates the tree.
//  Once game is finished, adds the win or loss statistics up the tree recursively.
    private boolean playTurn (Tree<TreeGameState> currentNode){
        List<Move> availableMoves = new ArrayList<>(currentNode.getValue().getGameState().getAvailableMoves());
        List<TreeGameState> childValues = new ArrayList<>(currentNode.getChildValues());

        Move nextMove = null;
//      Not all moves from this game state have been searched yet.
        if (childValues.size() < availableMoves.size()) {
//          Filters out all moves that have already been visited.
            List<Move> filteredAvailableMoves = removeMovesContaining(availableMoves, childValues);
            nextMove = filteredAvailableMoves.get(new Random().nextInt(filteredAvailableMoves.size()));
        }
//      All moves have been visited at least once, so we can use the UCT selection strategy.
        else {
            //Assume the first child is the best
            double bestScore = MCTS.processForUCB1(0.8, currentNode, childValues.get(0));
            nextMove = childValues.get(0).getPreviousMove();
            childValues.remove(0);

            for (TreeGameState childValue : childValues) {
                double currentChildScore = MCTS.processForUCB1(0.8, currentNode, childValue);
                if (currentChildScore > bestScore) {
                    bestScore = currentChildScore;
                    nextMove = childValue.getPreviousMove();
                }
            }
        }

        Board.GameState newGameState = currentNode.getValue().getGameState().advance(nextMove);
        TreeGameState newTreeGameState = new TreeGameState(newGameState, nextMove);
        Optional<Tree<TreeGameState>> optionalChild =
                this.gameStateTree.getChildNodeEqualling(newTreeGameState);

        Tree<TreeGameState> newTreeNode;
        // Returns the new Tree node created for value.
        if (optionalChild.isEmpty()) newTreeNode = currentNode.addChildValue(newTreeGameState);
        // Child contains a value
        else newTreeNode = optionalChild.get();

        boolean win;

        // Anchor case. Occurs once game has finished.
        if (!newTreeNode.getValue().getGameState().getWinner().isEmpty()) win = newGameState.getWinner().contains(MRX);
        // Recursive case. Traverse to the new tree node in the Tree<GameStateTree> (Plays another turn)
        else win = this.playTurn(newTreeNode);

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
