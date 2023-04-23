package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Manages the state for the Monte Carlo Tree Search.
 * Stores the current game state, as well as the recorded plays and wins on a state.
 * Wins are from perspective of parent, unless node is root node, where wins are from
 * perspective of itself.
 * Use of Node class to store all MCTS logic partially inspired from:
 * <a href="https://www.youtube.com/watch?v=wuSQpLinRB4">https://www.youtube.com/watch?v=wuSQpLinRB4</a>
 */
public class Node {
    private AIGameState gameState;
    private Move previousMove = null;
    final private Piece piece; // Either MrX or a Detective
    final private List<Move> remainingMoves; // Pre-filtered
    private double totalPlays;
    private double totalValue;

//  Used to reduce emphasis on nodes traversed by other threads.
    private int virtualLoss;

    final private List<Node> children;
    final private Node parent;
    final private Node root;
    private PossibleLocations possibleLocations;
    final private Heuristics.MoveFiltering moveFilter;
    final private Heuristics.CoalitionReduction coalitionReduction;
    final private Heuristics.ExplorationCoefficient explorationCoefficient;
    final private boolean notGameOver;

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
//                .filter(m -> m.commencedBy().equals(this.piece))
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
                 PossibleLocations possibleLocations,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction,
                 Heuristics.ExplorationCoefficient explorationCoefficient) {
        this.gameState = gameState;
        this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();
        this.root = this;
        this.parent = null;
        this.moveFilter = moveFilter;
        this.possibleLocations = possibleLocations;
        this.coalitionReduction = coalitionReduction;
        this.explorationCoefficient = explorationCoefficient;

        //Application of the move filtering heuristic
        this.remainingMoves = applyMoveFilterHeuristic(gameState);

        this.totalValue = 0;
        this.totalPlays = 0;
        this.children = new ArrayList<>(this.remainingMoves.size());
        this.notGameOver = this.gameState.getWinner().isEmpty();
    }


    /**
     * Constructor for non-root nodes
     * @param gameState Current game state for node.
     * @param root Root of tree
     * @param parent Parent of this node
     * @param previousMove Move that would traverse from the parent node to this node
     * */
    public Node (AIGameState gameState,
                 Node root,
                 Node parent,
                 Move previousMove,
                 PossibleLocations possibleLocations,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction,
                 Heuristics.ExplorationCoefficient explorationCoefficient) {

        this.gameState = gameState;
        this.root = root;
        this.parent = parent;
        this.previousMove = previousMove;
        this.possibleLocations = possibleLocations;
        this.moveFilter = moveFilter;
        this.coalitionReduction = coalitionReduction;
        this.explorationCoefficient = explorationCoefficient;

//      Win state reached (Can't expand anymore)
        if (!gameState.getWinner().isEmpty()) this.piece = parent.piece;
        else this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

        this.remainingMoves = applyMoveFilterHeuristic(gameState);
        this.children = new ArrayList<>(this.remainingMoves.size());

        this.totalValue = 0.0;
        this.totalPlays = 0.0;
        this.notGameOver = this.gameState.getWinner().isEmpty();
    }

    public Optional<AIGameState> getGameState () {
        return Optional.ofNullable(this.gameState);
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

    public int getVirtualLoss () {
        return this.virtualLoss;
    }

    public Node getParent (){ return this.parent; }

    /**
     * @return Current state of possible locations for game state.
     */
    public Optional<PossibleLocations> getPossibleLocations () {
        return Optional.ofNullable(this.possibleLocations);
    }

    /**
     * Gets the child with the most visits, meaning that the child is the most promising.
     * @return Child with the most visits.
     * @throws IllegalStateException if node has no children
     * */
    @Nonnull
    public synchronized Node getBestChild () {
        if (this.children.isEmpty()) throw new IllegalStateException("Cannot get best child of leaf node");

        // Post conditions will ensure score > -Infinity and bestChild will exist
        double bestScore = Double.NEGATIVE_INFINITY;
        Node bestChild = this.children.get(0);

        for (Node child : this.children) {
            double currentScore = child.getTotalPlays();
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestChild = child;
            }
        }

        return bestChild;
    }

    /**
     * Either expands and adds a child, or selects the best child, depending on if the node is fully
     * expanded.
     * @return Pair with child Node on left, and boolean (false if expanded, true if selected).
     */
    public synchronized Pair<Node, Boolean> expandOrSelect() {
//      Adds to virtual loss so that other threads visit different nodes.
        this.virtualLoss ++;
        if (this.isFullyExpanded()) {
            return new Pair<>(this.selectChild(), true);
        }
        else {
            return new Pair<>(this.expandNode(), false);
        }
    }

    public boolean isFullyExpanded () {
//      Checks if no available moves can be added and if there are children added.
        return this.remainingMoves.size() == 0 && this.children.size() > 0;
    }

    /**
     * Expansion stage of MCTS algorithm. Selects a random node from remaining moves.
     *
     * @return a new Node to add to the data structure (tree)
     * @throws IllegalStateException if this function tries to expand on a fully expanded node
     * */
    private Node expandNode () {
        if (remainingMoves.isEmpty()) throw new IllegalStateException("Cannot call expandNode on fully expanded node.");

        Move nextMove = remainingMoves.get(new Random().nextInt(remainingMoves.size()));
        remainingMoves.remove(nextMove);

        AIGameState newGameState = this.gameState.advance(nextMove);
        PossibleLocations newPossibleLocations = this.possibleLocations.updateLocations(newGameState);

        Node newNode = new Node(
                newGameState,
                this.root,
                this,
                nextMove,
                newPossibleLocations,
                this.moveFilter,
                this.coalitionReduction,
                this.explorationCoefficient
        );
        this.children.add(newNode);

//      Culls unnecessary data from node to reduce memory usage.
        if (remainingMoves.isEmpty()) {
            this.gameState = null;
            this.possibleLocations = null;
        }

        return newNode;
    }

    /**
     * Helper function to selectChild. Calculate the Upper Confidence Bound for the node.
     * This is used to focus on nodes which are more promising and avoid less promising
     * nodes.
     *
     * @param childNode childNode to evaluate UCB on
     * @return Evaluation of the UCB1 equation of the child node
     * @throws IllegalArgumentException If child is not defined
     * @throws IllegalArgumentException If the childNode given as parameter is not the child of the node.
     * */
    private double calculateUCB (Node childNode) {
        double EXPLORATION_VALUE = this.piece.isMrX() ?
                this.explorationCoefficient.getMrXCoefficient() :
                this.explorationCoefficient.getDetectiveCoefficient();

        Objects.requireNonNull(childNode, "Child node not defined");

        if (!this.children.contains(childNode))
            throw new IllegalArgumentException("Node not child of current node.");

        double avgScore;
//      Avoid divide by 0 error.
        if (childNode.getTotalPlays() == 0) {
            avgScore = 0;
        } else {
//          Subtracts virtual loss so that threads are less likely to traverse this path when
//          already visited by another thread.
            avgScore = (childNode.getTotalValue() - childNode.virtualLoss) / childNode.getTotalPlays();
        }

        double explorationFactor =
                EXPLORATION_VALUE * (Math.sqrt(
                        Math.log(this.getTotalPlays())
                                / childNode.getTotalPlays()
                ));

        return avgScore + explorationFactor;
    }


    /**
     * Select child based on the best UCB score
     * @throws IllegalStateException node has no children
     * */
    private Node selectChild () {
        if (this.children.isEmpty())
            throw new IllegalStateException("Cannot select child as no children exist");

//      This is safe since it will always be overwritten in the for loop.
        Node bestChild = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Node child : this.children){
            double currentUCB = this.calculateUCB(child);
            if (bestScore < currentUCB) {
                bestScore = currentUCB;
                bestChild = child;
            }
        }
        return bestChild;
    }

    public boolean isNotGameOver() {
        return this.notGameOver;
    }


    /**
     * Extract a winner from a given gameState. Will return Empty is there doesn't exist a winner
     *
     * @throws IllegalArgumentException if given gameState is an initial game state
     * */
    static public Optional<Piece> getGameWinner (AIGameState gameState) {
        if (gameState.getWinner().isEmpty())
            return Optional.empty();
        else if (gameState.getWinner().asList().get(0).isMrX())
            return Optional.of(Piece.MrX.MRX);
        else
            return gameState.getPreviousMove().map(Move::commencedBy);
    }

    /**
     * Simulates a game from the current game state.
     *
     * @param gameState The current game state from a Node.
     * @param possibleLocations The current Set of possible locations for Mr X.
     *                          Used for E-Greedy Playouts.
     * @param eGreedyPlayouts A EGreedyPlayouts class to define how moves should be picked.
     * @return Value of simulated game (winning piece)
     */
    public static Piece simulateGame (
            AIGameState gameState,
            PossibleLocations possibleLocations,
            Heuristics.EGreedyPlayouts eGreedyPlayouts) {
        AIGameState currentGameState = gameState;
        PossibleLocations currentPossibleLocations = possibleLocations;

        //Anchor case
        if (Node.getGameWinner(currentGameState).isPresent())
            return Node.getGameWinner(currentGameState).get();

        while (currentGameState.getWinner().isEmpty()) {
            Move move;
            if (new Random().nextDouble() > eGreedyPlayouts.EPSILON) {
                if (currentGameState.getAvailableMoves().asList().get(0).commencedBy().isMrX()) {
                    move = eGreedyPlayouts.getMrXBestMove(
                        currentGameState.getAvailableMoves(),
                        currentGameState
                    );
                } else {
                    move = eGreedyPlayouts.getDetectiveBestMove(
                        currentGameState.getAvailableMoves(),
                        currentPossibleLocations
                    );
                }
            } else {
                move = currentGameState.getAvailableMoves().asList().get(
                        new Random().nextInt(currentGameState.getAvailableMoves().size())
                );
            }

            currentGameState = currentGameState.advance(move);
            currentPossibleLocations = currentPossibleLocations.updateLocations(currentGameState);
        }

        return Node.getGameWinner(currentGameState).orElseThrow();
    }

    /**
     * Back-propagates result from simulated game up the tree to the root.
     * @param value The Piece which won the simulated game
     * @return Recurse up tree and returns value at root of tree.
     */
    public Piece backPropagation(Piece value) {
//      Ensures that the count is only updated on node by single thread at a time.
        synchronized (this) {
            this.totalPlays += 1;
            this.virtualLoss --;

            //      Root node
            if (this.parent == null) {
                this.totalValue += this.coalitionReduction.calculateValue(this.piece, value);
                return value;
            } else {
                this.totalValue += this.coalitionReduction.calculateValue(this.parent.piece, value);
            }
        }

//      Recurse value to top of tree.
        return this.parent.backPropagation(value);
    }
}
