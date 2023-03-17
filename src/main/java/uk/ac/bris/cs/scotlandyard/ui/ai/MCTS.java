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
    private final double EXPLORATION_FACTOR;

    private enum EndState {
        WIN,
        LOSS,
        NONE
    }

    public MCTS (Tree<TreeGameState> tree, double explorationFactor) {
        this.gameStateTree = tree;
        this.EXPLORATION_FACTOR = explorationFactor;

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
    static double processForUCB1 (double explorationFactor, Tree<TreeGameState> parent ,TreeGameState child){
        double np = parent.getValue().getTotalPlays();
        double ni = child.getTotalPlays();
        double wi = child.getWins();

        return calculateUCB1(explorationFactor, np, ni, wi);
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

//  Recursively moves down the tree until a node without all the possible child states is reached.
//  Then, playTurn will be called and the game will be simulated to the end.
    private EndState traverseTree (Tree<TreeGameState> currentNode) {
        List<Move> availableMoves = new ArrayList<>(currentNode.getValue().getGameState().getAvailableMoves());
        List<Tree<TreeGameState>> childNodes = new ArrayList<>(currentNode.getChildNodes());
        Tree<TreeGameState> nextChild = null;
        EndState win;

//      All moves that can be carried out have already been added to the tree.
        if (childNodes.size() == availableMoves.size()) {
//          Win state reached already so path not counted.
            if (availableMoves.size() == 0) {
                return EndState.NONE;
            }

//          Assume the first child is the best
            nextChild = childNodes.get(0);
            double bestScore = MCTS.processForUCB1(this.EXPLORATION_FACTOR ,currentNode, nextChild.getValue());
            childNodes.remove(0);

            for (Tree<TreeGameState> childNode : childNodes) {
                double currentChildScore = MCTS.processForUCB1(this.EXPLORATION_FACTOR, currentNode, childNode.getValue());
                if (currentChildScore > bestScore) {
                    bestScore = currentChildScore;
                    nextChild = childNode;
                }
            }

            win = this.traverseTree(nextChild);
        }
//      Not all moves from this game state have been searched yet.
        else {
            Board.GameState currentState = currentNode.getValue().getGameState();

//          Filter to only non-checked moves and select random non-played move.
            List<Move> filteredAvailableMoves = removeMovesContaining(availableMoves, currentNode.getChildValues());
            Move nextMove = filteredAvailableMoves.get(new Random().nextInt(filteredAvailableMoves.size()));

//          Play randomly selected move and create new Tree node for game state.
            Board.GameState nextState = currentState.advance(nextMove);
            nextChild = currentNode.addChildValue(new TreeGameState(nextState, nextMove));

            win = this.playTurn(nextState);
        }

        if (win.equals(EndState.WIN)) nextChild.getValue().addWin();
        else if (win.equals(EndState.LOSS)) nextChild.getValue().addLoss();

        return win;
    }

//  Recursively plays a turn (from available moves from currentNode's game state) and updates the tree.
//  Once game is finished, adds the win or loss statistics up the tree recursively.
    private EndState playTurn (Board.GameState currentState){
//      If there is a winner, then end recursion and pass win boolean up recursion stack.
        if (!currentState.getWinner().isEmpty()) {
//          If MrX is winner, return true, else return false.
            if (currentState.getWinner().contains(MRX)) {
                return EndState.WIN;
            } else {
                return EndState.LOSS;
            }
        }

        List<Move> availableMoves = currentState.getAvailableMoves().asList();
        Move randomMove = availableMoves.get(new Random().nextInt(availableMoves.size()));

        return this.playTurn(currentState.advance(randomMove));
    }

    //Main component to execute the algorithm
    @Override
    public void run () {
        EndState currentWin;
        while(!Thread.interrupted()) {
            currentWin = this.traverseTree(this.gameStateTree);
            if (currentWin.equals(EndState.WIN)) this.gameStateTree.getValue().addWin();
            else if (currentWin.equals(EndState.LOSS)) this.gameStateTree.getValue().addLoss();
        }
    }

}
