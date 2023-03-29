package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Our own node data structure
public class Node {
    final private AIGameState gameState;
    private Move previousMove = null;
    final private Piece piece; //Either MrX or a Detective
    private List<Move> remainingMoves; //Pre-filtered
    private double totalPlays;
    private double totalValue;
    private ConcurrentHashMap<Integer, Node> children;
    private Node parent = null;
    final private Node root;
    final private double EXPLORATION_VALUE = 0.8;
    final private Heuristics.MoveFiltering moveFilter;
    final private Heuristics.CoalitionReduction coalitionReduction;

    /**
     * Helper function to Constructors
     * @param gameState Current game state
     * @return Filtered list of moves possible from current game state
     * */
    private List<Move> applyMoveFilterHeuristic(AIGameState gameState){
        return new ArrayList<>(gameState
                .getAvailableMoves()
                .asList()
                .stream()
                .filter(m -> m.commencedBy().equals(this.piece))
                .filter(m -> this.moveFilter.checkMove(m, gameState))
                .toList());
    }

    /**
     * Constructor for the root node
     * @param gameState Current game state
     * @param moveFilter move filtering heuristic
     * @param coalitionReduction coalitionReduction heuristic
     * */
    public Node (AIGameState gameState,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction) {
        this.gameState = gameState;
        this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();
        this.root = this;
        this.moveFilter = moveFilter;
        this.coalitionReduction = coalitionReduction;

        System.out.println("Current turn: " + this.piece);

        //Application of the move filtering heuristic
        this.remainingMoves = applyMoveFilterHeuristic(gameState);

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
    public Node (AIGameState gameState,
                 Node root,
                 Node parent,
                 Move previousMove,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction) {
        this.gameState = gameState;
        this.root = root;
        this.parent = parent;
        this.previousMove = previousMove;
        this.children = new ConcurrentHashMap<>();
        this.moveFilter = moveFilter;
        this.coalitionReduction = coalitionReduction;

//      Win state reached (Can't expand anymore)
        if (!gameState.getWinner().isEmpty()) this.piece = parent.piece;
        else this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

        this.remainingMoves = applyMoveFilterHeuristic(gameState);

        this.totalValue = 0.0;
        this.totalPlays = 0.0;
    }

    public AIGameState getGameState () {
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

        AIGameState newGameState = this.gameState.advance(nextMove);
        Node newNode = new Node(newGameState, this.root, this, nextMove, this.moveFilter, this.coalitionReduction);
        this.children.put(newGameState.hashCode(), newNode);

        return newNode;
    }

    /**
     * Helper function to selectChild
     * @param childNode childNode to evaluate UCB on
     * @return evaluation of the UCB1 equation of the child node
     * @throws IllegalArgumentException if child is not defined
     * @throws IllegalArgumentException if the childNode given as parameter is not the child of the node.
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


    /**
     * Select child based on the best UCB score
     * @throws IllegalStateException node has no children
     * */
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

    public boolean isNotGameOver() {
        return this.getGameState().getWinner().isEmpty();
    }


    /**
     * Extract a winner from a given gameState
     * @throws IllegalArgumentException if given gameState is an initial game state
     * */
    static public Optional<Piece> getGameWinner (AIGameState gameState) {
        if (gameState.getWinner().isEmpty()) return Optional.empty();
        else if (gameState.getWinner().asList().get(0).isMrX()) return Optional.of(Piece.MrX.MRX);
        else {
            Optional<Move> optionalMove = gameState.getPreviousMove();
            if (optionalMove.isEmpty()) {
                throw new IllegalArgumentException("Cannot get detective winner of initial game state");
            }
            return Optional.of(optionalMove.get().commencedBy());
        }
    }

    // Simulation/Playoff stage
    public Piece simulateGame () {
        AIGameState currentGameState = this.gameState;

        //Anchor case
        if (this.getGameWinner(currentGameState).isPresent())
            return this.getGameWinner(currentGameState).get();

        while (currentGameState.getWinner().isEmpty()) {
            Move randomMove = currentGameState.getAvailableMoves()
                    .asList()
                    .get(
                            new Random().nextInt(currentGameState.getAvailableMoves().size())
                    );
            currentGameState = currentGameState.advance(randomMove);
        }

        Optional<Piece> optionalWinner = Node.getGameWinner(currentGameState);
        if (optionalWinner.isEmpty())
            throw new IllegalStateException("Cannot get winner for winning game state");

        //Recursive case
        return optionalWinner.get();
    }

    // Backpropagation stage
    public Piece backPropagation(Piece value) {
        this.totalPlays += 1;

        //Apply Coalition Reduction
        this.totalValue += this.coalitionReduction.calculateValue(this.root.piece, value);

//      Root node
        if (this.parent == null) return value;

        // Upwards recursion
        return this.parent.backPropagation(value);
    }
}
