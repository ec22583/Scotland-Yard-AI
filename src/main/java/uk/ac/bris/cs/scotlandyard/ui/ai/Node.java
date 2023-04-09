package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

// Our own node data structure
public class Node {
    final private AIGameState gameState;
    private Move previousMove = null;
    final private Piece piece; //Either MrX or a Detective
    final private List<Move> remainingMoves; //Pre-filtered
    private double totalPlays;
    private double totalValue;
    final private List<Node> children;
    private Node parent = null;
    final private Node root;
    final private PossibleLocations possibleLocations;
    final private Heuristics.MoveFiltering moveFilter;
    final private Heuristics.CoalitionReduction coalitionReduction;
    final private Heuristics.ExplorationCoefficient explorationCoefficient;
    final private Heuristics.EGreedyPlayouts eGreedyPlayouts;

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
                 PossibleLocations possibleLocations,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction,
                 Heuristics.ExplorationCoefficient explorationCoefficient,
                 Heuristics.EGreedyPlayouts eGreedyPlayouts) {
        this.gameState = gameState;
        this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();
        this.root = this;
        this.moveFilter = moveFilter;
        this.possibleLocations = possibleLocations;
        this.coalitionReduction = coalitionReduction;
        this.explorationCoefficient = explorationCoefficient;
        this.eGreedyPlayouts = eGreedyPlayouts;

        System.out.println("Current turn: " + this.piece);

        //Application of the move filtering heuristic
        this.remainingMoves = applyMoveFilterHeuristic(gameState);

        this.totalValue = 0;
        this.totalPlays = 0;
        this.children = new ArrayList<>(this.remainingMoves.size());
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
                 PossibleLocations possibleLocations,
                 Heuristics.MoveFiltering moveFilter,
                 Heuristics.CoalitionReduction coalitionReduction,
                 Heuristics.ExplorationCoefficient explorationCoefficient,
                 Heuristics.EGreedyPlayouts eGreedyPlayouts) {

        this.gameState = gameState;
        this.root = root;
        this.parent = parent;
        this.previousMove = previousMove;
        this.possibleLocations = possibleLocations;
        this.moveFilter = moveFilter;
        this.coalitionReduction = coalitionReduction;
        this.explorationCoefficient = explorationCoefficient;
        this.eGreedyPlayouts = eGreedyPlayouts;

//      Win state reached (Can't expand anymore)
        if (!gameState.getWinner().isEmpty()) this.piece = parent.piece;
        else this.piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

        this.remainingMoves = applyMoveFilterHeuristic(gameState);
        this.children = new ArrayList<>(this.remainingMoves.size());

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
        return ImmutableList.copyOf(this.children);
    }

    /**
     * @return the child with the best win rate
     * @throws IllegalStateException if there are no children
     * */
    public Node getBestChild () {
        // Post conditions will ensure score > -Infinity and bestChild will exist
        double bestScore = Double.NEGATIVE_INFINITY;
        Node bestChild = null;

        if (this.children.isEmpty()) throw new IllegalStateException("Cannot get best child of leaf node");

        for (Node child : this.children) {
            double currentScore = child.getTotalPlays();
            if (currentScore > bestScore) {
                System.out.println("Found child with higher plays: " + child.getPreviousMove() + " " + child.getTotalPlays());
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

        PossibleLocations newPossibleLocations = this.possibleLocations.updateLocations(newGameState);

        Node newNode = new Node(
                newGameState,
                this.root,
                this,
                nextMove,
                newPossibleLocations,
                this.moveFilter,
                this.coalitionReduction,
                this.explorationCoefficient,
                this.eGreedyPlayouts
        );
        this.children.add(newNode);

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
        double EXPLORATION_VALUE = this.piece.isMrX() ?
                this.explorationCoefficient.getMrXCoefficient() :
                this.explorationCoefficient.getDetectiveCoefficient();

        if (childNode == null) throw new IllegalArgumentException("Child node not defined.");
        if (!this.children.contains(childNode))
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

//        // If player is a detective.
//        if ( !this.getPiece().equals(this.root.getPiece()) ) {
//            avgScore = 1 - avgScore;
//        }

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
        PossibleLocations currentPossibleLocations = this.possibleLocations;

        //Anchor case
        if (Node.getGameWinner(currentGameState).isPresent())
            return Node.getGameWinner(currentGameState).get();

        while (currentGameState.getWinner().isEmpty()) {
            Move move;
            if (new Random().nextDouble() > 0.2) {
                if (currentGameState.getAvailableMoves().asList().get(0).commencedBy().isMrX()) {
                    move = this.eGreedyPlayouts.getMrXBestMove(
                        currentGameState.getAvailableMoves(),
                        currentGameState
                    );
                } else {
                    move = this.eGreedyPlayouts.getDetectiveBestMove(
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

        //Recursive case
        return Node.getGameWinner(currentGameState).orElseThrow();
    }

    // Backpropagation stage
    public Piece backPropagation(Piece value) {
        this.totalPlays += 1;

//      Root node
        if (this.parent == null) {
            this.totalValue += this.coalitionReduction.calculateValue(this.piece, value);
            return value;
        } else {
//          Apply Coalition Reduction
            this.totalValue += this.coalitionReduction.calculateValue(this.parent.piece, value);
//            if (this.parent.parent == null && this.root != this) {
//                System.out.println("--------------------------------------------------------------------------");
//                System.out.println(String.format("Winner of game: %s, Parent of node: %s, Root of node: %s Value added: %s", value, this.parent.piece, this.root.piece, this.coalitionReduction.calculateValue(this.parent.piece, value)));
//                System.out.println(String.format("Move: %s, New plays: %s, New value: %s", this.getPreviousMove(), this.getTotalPlays(), this.getTotalValue()));
//            }


//          Upwards recursion
            return this.parent.backPropagation(value);
        }

    }
}
