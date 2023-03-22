package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.Opt;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Our own node data structure
public class Node {
    final private Board.GameState gameState;
    private Move previousMove = null;
    final private Piece piece; //Either MrX or a Detective
    private List<Move> remainingMoves; //Pre-filtered
    private double totalPlays;
    private double totalValue;
    private ConcurrentHashMap<Integer, Node> children;
    private Node parent = null;
    final private Node root;
    final private double EXPLORATION_VALUE = 0.8;

//  Used to record who has won the game.
    public enum GameValue {
        MRX,
        BLUE,
        RED,
        GREEN,
        YELLOW,
        WHITE,
        NONE;

        public Optional<Piece> getWinningPiece () {
            switch (this) {
                case MRX ->     { return Optional.of(Piece.MrX.MRX); }
                case BLUE ->    { return Optional.of(Piece.Detective.BLUE); }
                case RED ->     { return Optional.of(Piece.Detective.RED); }
                case GREEN ->   { return Optional.of(Piece.Detective.GREEN); }
                case YELLOW ->  { return Optional.of(Piece.Detective.YELLOW);}
                case WHITE ->   { return Optional.of(Piece.Detective.WHITE);}
                default ->      { return Optional.empty(); }
            }
        }
    }

    /**
     * Constructor for the root node
     * @param gameState Current game state
     * */
    public Node (Board.GameState gameState) {
        this.gameState = gameState;
        this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();
        this.root = this;

        System.out.println("Current turn: " + this.piece);

        this.remainingMoves = new ArrayList<>(gameState
                .getAvailableMoves()
                .asList()
                .stream()
                .filter(m -> m.commencedBy().equals(this.piece))
                .toList()
        );
        this.totalValue = 0;
        this.totalPlays = 0;
        this.children = new ConcurrentHashMap<>();
    }


    /**
     * Constructor for non-root nodes
     * @param gameState Current game state
     * @param root root of the data structure (tree)
     * @param parent parent of this node
     * @param previousMove move that would traverse from the parent node to this node
     * */
    public Node (Board.GameState gameState, Node root, Node parent, Move previousMove) {
        this.gameState = gameState;
        this.root = root;
        this.parent = parent;
        this.previousMove = previousMove;
        this.children = new ConcurrentHashMap<>();

//      Win state reached (Can't expand anymore)
        if (!gameState.getWinner().isEmpty()) this.piece = parent.piece;
        else this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

        this.remainingMoves = new ArrayList<>(gameState
                .getAvailableMoves()
                .asList()
                .stream()
                .filter(m -> m.commencedBy().equals(this.piece))
                .toList());

        this.totalValue = 0;
        this.totalPlays = 0;
    }

    public Board.GameState getGameState () {
        return this.gameState;
    }

    public Piece getPiece () {
        return this.piece;
    }

    public Move getPreviousMove () {
        return this.previousMove;
    }

    public double getTotalValue () {
        return this.totalValue;
    }

    public double getTotalPlays () {
        return this.totalPlays;
    }

    public List<Node> getChildren (){
        return ImmutableList.copyOf(this.children.values());
    }

    /**
     * @return the child with the best win rate
     * @throws IllegalStateException if there are no children
     * */
    public Node getBestChild () {
        // Post conditions will ensure score > -Infinity and bestChild will exist
        double bestScore = Double.NEGATIVE_INFINITY;
        Node bestChild = null;

        if (this.children.size() == 0) throw new IllegalStateException("Cannot get best child of leaf node");

        for (Node child : this.children.values()) {
            double currentScore = child.getTotalValue()/child.getTotalPlays();
            if (currentScore > bestScore) {
                System.out.println("Found child with higher score: " + child.getPreviousMove());
                bestScore = currentScore;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public boolean isFullyExpanded () {
//      Checks if no available moves can be added and if there are children added.
        return this.remainingMoves.size() == 0 && this.children.size() > 0;
    }

    /**
     * Expansion stage of MCTS algorithm. Selects a random node from remaining moves.
     * @return a new Node to add to the data structure (tree)
     * @throws IllegalStateException if this function tries to expand on a fully expanded node
     * */
    public Node expandNode () {
        if (remainingMoves.size() == 0) throw new IllegalStateException("Cannot call expandNode on fully expanded node.");

        Move nextMove = remainingMoves.get(new Random().nextInt(remainingMoves.size()));
        remainingMoves.remove(nextMove);

        Board.GameState newGameState = this.gameState.advance(nextMove);
        Node newNode = new Node(newGameState, this.root, this, nextMove);
        this.children.put(newGameState.hashCode(), newNode);

        return newNode;
    }

    //Helper function
    /**
     *
     * @param childNode childNode to evaluate UCB on
     * @return evaluation of the UCB1 equation of the child node
     * */
    private double calculateUCB (Node childNode) {
        if (childNode == null) throw new IllegalArgumentException("Child node not defined.");
        if (!this.children.containsKey(childNode.getGameState().hashCode()))
            throw new IllegalArgumentException("Node not child of current node.");

        double avgScore = childNode.getTotalValue() / childNode.getTotalPlays();
        if (childNode.getTotalPlays() == 0) {
            avgScore = 0;
        }

        double explorationFactor =
                EXPLORATION_VALUE * (Math.sqrt(
                        Math.log(this.getTotalPlays())
                                / childNode.getTotalPlays()
                ));

        // If player is a detective.
        if ( this.getPiece().isDetective() ) {
            avgScore = 1 - avgScore;
        }

        return avgScore + explorationFactor;
    }

    // select child based on best UCB score
    public Node selectChild () {
        if (this.children.size() == 0) throw new IllegalStateException("Cannot select child as no children exist");

        double bestScore = Double.NEGATIVE_INFINITY;
//      This is safe since it will always be overwritten in the for loop.
        Node bestChild = null;

        for (Node child : this.children.values()){
            double currentUCB = this.calculateUCB(child);
            if (bestScore < currentUCB) {
                bestScore = currentUCB;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public boolean isGameOver () {
        return !this.getGameState().getWinner().isEmpty();
    }

    static public GameValue getGameWinner (Board.GameState gameState) {
        System.out.println("Current winner list: " + gameState.getWinner());
        if (gameState.getWinner().isEmpty()) return GameValue.NONE;
        else if (gameState.getWinner().asList().get(0).isMrX()) return GameValue.MRX;
        else return GameValue.BLUE;
    }

    // Simulation/Playoff stage
    public GameValue simulateGame () {
        Board.GameState currentGameState = this.gameState;

        //Anchor case
        if (this.isGameOver()) return this.getGameWinner(currentGameState);

        while (currentGameState.getWinner().isEmpty()) {
            Move randomMove = currentGameState.getAvailableMoves()
                    .asList()
                    .get(
                            new Random().nextInt(currentGameState.getAvailableMoves().size())
                    );
            currentGameState = currentGameState.advance(randomMove);
        }

        //Recursive case
        return this.getGameWinner(currentGameState);
    }

    private Optional<Double> calculateValue (GameValue value) {
        if (value.equals(GameValue.NONE)) return Optional.empty();

        // If the root is MrX and checking he wins or not
        if (this.root.piece.equals(Piece.MrX.MRX)) {
            if (value.equals(GameValue.MRX)) return Optional.of(1.0);
            else return Optional.of(-1.0);
        } else {
            if (value.equals(GameValue.MRX)) return Optional.of(-1.0);
            else return Optional.of(1.0);
        }
    }


    // Backpropagation stage
    public GameValue backPropagation(GameValue value) {
        this.totalPlays += 1;

        Optional<Double> doubleOptional = this.calculateValue(value);
        if (doubleOptional.isEmpty())
            throw new IllegalArgumentException("Trying to back propagate a non winning state");

        this.totalValue += doubleOptional.get();

//      Root node
        if (this.parent == null) return value;

        // Upwards recursion
            return this.parent.backPropagation(value);
    }
}
