package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// MCTS Algorithm (Monte Carlo tree search
public class MCTS {
    private Tree<TreeGameState> gameStateTree;
    private Board.GameState startGameState;

    private boolean playTurn (Tree<TreeGameState> currentNode){
        List<Move> availableMoves = currentNode.getValue().getGameState().getAvailableMoves().asList();
        Move randomMove = availableMoves.get(new Random().nextInt(availableMoves.size()));

        Board.GameState newGameState = currentNode.getValue().getGameState().advance(randomMove);
        TreeGameState newTreeGameState = new TreeGameState(newGameState, randomMove);
        Optional<Tree<TreeGameState>> optionalChild =
                this.gameStateTree.getChildNodeEqualling(newTreeGameState);

        Tree<TreeGameState> newTreeNode;
        if (optionalChild.isEmpty()) {
            newTreeNode = new Tree<>(newTreeGameState);
            currentNode.addChildNode(newTreeNode);
        }
        else newTreeNode = optionalChild.get(); //child Contains a value

        boolean win;
        if (!newTreeNode.getValue().getGameState().getWinner().isEmpty()) {
//          Anchor case.
            win = newGameState.getWinner().contains(MRX);
        }
        else {
//          Recursive case.
            win = this.playTurn(newTreeNode);
        }

        if (win) currentNode.getValue().addWin();
        else currentNode.getValue().addLoss();

        return win;
    }

    public void updateGameState(Board.GameState gameState){
        this.startGameState = gameState;
    }

    public Tree<TreeGameState> run () {
        this.gameStateTree = new Tree<>(new TreeGameState(this.startGameState));

        //How many tree explorations AI should perform
        for (int i = 0; i < 3000; i++) {
            this.playTurn(this.gameStateTree);
        }

        return this.gameStateTree;
    }
}
