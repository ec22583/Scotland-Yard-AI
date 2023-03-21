package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public class GameStateFactory {
    //Helper function
    static private ImmutableList<Player> getDetectives (Board board){

        List<Player> detectives = new LinkedList<Player>(board
                .getPlayers()
                .stream()
                .filter(p -> p.isDetective())
                .map(piece -> {
                    Optional<Integer> locationOptional = board.getDetectiveLocation((Piece.Detective) piece);
                    if (locationOptional.isEmpty())
                        throw new IllegalStateException("Detective location not available.");

                    return new Player(
                                    piece,
                                    // Generates tickets for piece.
                                    BoardHelpers.getTicketsForPlayer(board, piece),
                                    //  Piece must be cast to a Detective. Not an issue since mrx filtered out earlier
                                    // (For type safety). .get() fine as piece always is a detective.
                                    locationOptional.get()
                            );
                        }
                )
                .toList()
        );
        return ImmutableList.copyOf(detectives);
    }

    public Board.GameState generateMrXGameState (Board board) {
        if (board.getAvailableMoves().isEmpty())
            throw new IllegalArgumentException("Board already winning board.");

        GameSetup gameSetup = BoardHelpers.generateGameSetup(board);

        int location = board
                .getAvailableMoves()
                .asList()
                .get(0)
                .source();


        Player mrX = new Player(MRX, BoardHelpers.getTicketsForPlayer(board, MRX) , location);
        ImmutableList<Player> detectives = GameStateFactory.getDetectives(board);

        MyGameStateFactory myGameStateFactory = new MyGameStateFactory();

        //Construct a new game state with parameters above
        Board.GameState gameState = myGameStateFactory.build(gameSetup, mrX, detectives);

        return gameState;
    }

    /**
     * Create a player with an added ticket move to a different location
     * Add the move to a list so we can move the game states
     * @param board Current game state
     * @param piece Piece being inspected
     * @param currentLocation current location
     * @return The adjusted player and the move to return the player to the original position
     * @throws IllegalStateException If a node has no edges
     * @throws IllegalStateException If the transport for edge cannot be found
     * */
    private static Pair<Player, Move> getRevisedPlayer (Board board, Piece piece, int currentLocation) {
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;

        Set<EndpointPair<Integer>> adjacentEdges = graph.incidentEdges(currentLocation);
        Optional<EndpointPair<Integer>> optionalEdge = adjacentEdges.stream().findAny();
        if (optionalEdge.isEmpty()) throw new IllegalStateException("Node has no edges");
        EndpointPair<Integer> edge = optionalEdge.get();

        int destination = edge.adjacentNode(currentLocation);
        Optional<ImmutableSet<ScotlandYard.Transport>> optionalTransports = graph.edgeValue(edge);
        if (optionalTransports.isEmpty()) throw new IllegalStateException("Cannot find transports for edge");

        //Filter out any secret tickets
        List<ScotlandYard.Transport> filteredTransports = optionalTransports
                .get()
                .stream()
                .filter(t -> !t.requiredTicket().equals(ScotlandYard.Ticket.SECRET))
                .toList();

        //Get the required ticket from the first transportation type in the list
        ScotlandYard.Ticket ticket = filteredTransports.get(0).requiredTicket();

        Player player = new Player(
                piece,
                BoardHelpers.getTicketsForPlayer(board, piece),
                destination
        );
        player = player.give(ticket);
        Move move = new Move.SingleMove(piece, destination, ticket, currentLocation);

        return new Pair<Player, Move>(player, move);
    }

    /**
     * Generates predicated game states of MrX from detectives' perspective.
     * @param board Current Game State
     * @param possibleLocations Current Possible Positions for Mr X.
     * @return List of possible game states from detectives' perspectives.
     * @throws IllegalStateException When it takes a detective but can't find a location of the detective
     */
    public List<Board.GameState> generateDetectiveGameStates (Board board, PossibleLocations possibleLocations){
        //Setup variables
        MyGameStateFactory myGameStateFactory = new MyGameStateFactory();
        List<Board.GameState> possibleGameStates = new ArrayList<>(possibleLocations.getLocations().size());

        //List of pieces whose turns have already passed
        List<Piece> pieceTurnsPassed = new LinkedList<>();
        pieceTurnsPassed.addAll(board.getPlayers());
        List<Piece> piecesRemaining = new LinkedList<>();
        List<Player> detectivePlayers = new LinkedList<>();
        List<Move> moves = new LinkedList<>();

        //Checks across all moves to see which piece haven't yet moved and removes them from pieceTurnsPassed
        for (Move move : board.getAvailableMoves()) {
            if (!piecesRemaining.contains(move.commencedBy())) {
                piecesRemaining.add(move.commencedBy());
            }
        }
        pieceTurnsPassed.removeAll(piecesRemaining);

        //Extracts detective pieces (that have already moved)
        List<Piece.Detective > detectives = pieceTurnsPassed.stream()
                .filter(p -> p.isDetective())
                .map(p -> (Piece.Detective) p)
                .toList();

        //Create a detective with an added ticket move to a different location
        //Add the move to a list, so we can move game states
        for (Piece.Detective detective : detectives) {
            Optional<Integer> detectiveLocationOptional = board.getDetectiveLocation(detective);
            if (detectiveLocationOptional.isEmpty())
                throw new IllegalStateException("Cannot find location for detective");
            int detectiveLocation = detectiveLocationOptional.get();

            Pair<Player, Move> revisedPlayer =
                    GameStateFactory.getRevisedPlayer(board, detective, detectiveLocation);
            detectivePlayers.add(revisedPlayer.left());
            moves.add(revisedPlayer.right());

        }

        // Generate possible game states from MrX from his possible moves deduced by detectives
        for (int possibleLocation : possibleLocations.getLocations()) {
            Pair<Player, Move> revisedMrX = GameStateFactory.getRevisedPlayer(board, MRX, possibleLocation);

            List<Move> updatedMoves = moves;
            updatedMoves.add(revisedMrX.right());
            Player mrX = revisedMrX.left();

            Board.GameState newGameState = myGameStateFactory.build(
                    BoardHelpers.generateGameSetup(board),
                    mrX,
                    (ImmutableList<Player>) detectivePlayers
            );

            //Update the game state to advance with the move
            for (Move move : moves) {
                newGameState = newGameState.advance(move);
            }
            possibleGameStates.add(newGameState);
        }

        return possibleGameStates;
    }
}
