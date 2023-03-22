package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.nullness.Opt;
import org.glassfish.grizzly.Transport;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.*;
import java.util.List;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public class GameStateFactory {

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
        ImmutableList<Player> detectives = BoardHelpers.getDetectives(board);

        MyGameStateFactory myGameStateFactory = new MyGameStateFactory();

        //Construct a new game state with parameters above
        Board.GameState gameState = myGameStateFactory.build(gameSetup, mrX, detectives);

        return gameState;
    }

    //TODO: REFACTOR THIS INTO SMALLER HELPER FUNCTIONS (for increased readability)

    /**
     * Create a player with an added ticket move to a different location
     * Add the move to a list so that we can move the game states
     * @param board Current game state
     * @param piece Piece being inspected
     * @param currentLocation current location
     * @return The adjusted player and the move to return the player to the original position
     * @throws IllegalStateException If a node has no edges
     * @throws IllegalStateException If the transport for edge cannot be found
     * */
    private static Pair<Player, Move> getRevisedPlayer (
            Board board,
            Piece piece,
            int currentLocation,
            int mrXLocation,
            List<Player> otherPlayers) {
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph = board.getSetup().graph;
        List<Integer> alreadyUsedLocations = otherPlayers.stream().map(Player::location).toList();

        Set<EndpointPair<Integer>> adjacentEdges = graph.incidentEdges(currentLocation);
        List<EndpointPair<Integer>> edgesList = new LinkedList<>(adjacentEdges);
        if (edgesList.isEmpty()) throw new IllegalStateException("Node has no edges");

        List<Integer> destinations = new LinkedList<>(adjacentEdges
                .stream()
                .filter(e -> {
                    Optional<ImmutableSet<ScotlandYard.Transport>> optionalEdge = graph.edgeValue(e);
                    if (optionalEdge.isEmpty()) throw new IllegalStateException("Cannot find tickets for edge");
                    List<ScotlandYard.Ticket> tickets =
                            optionalEdge
                                    .get()
                                    .stream()
                                    .map(t -> t.requiredTicket())
                                    .toList();
                    System.out.println(tickets);
                    return !tickets.contains(ScotlandYard.Ticket.SECRET);
                })
                .map(e -> e.adjacentNode(currentLocation))
                .toList());

        destinations.removeAll(alreadyUsedLocations);
        destinations.removeAll(BoardHelpers.getDetectiveLocations(board));
        if (piece.isDetective()) {
            destinations.remove((Integer) mrXLocation);
        }
        int destination = destinations.get(new Random().nextInt(destinations.size()));

        Optional<ImmutableSet<ScotlandYard.Transport>> optionalTransports = graph.edgeValue(currentLocation, destination);
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
                currentLocation
        );
        player = player.at(destination);
        player = player.give(ticket);

        Move move = new Move.SingleMove(piece, destination, ticket, currentLocation);

        return new Pair<Player, Move>(player, move);
    }


    //TODO: REFACTOR THIS INTO SMALLER HELPER FUNCTIONS (for increased readability)

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

        // Generate possible game states from MrX from his possible moves deduced by detectives
        for (int possibleLocation : possibleLocations.getLocations()) {
            List<Player> detectivePlayers = new LinkedList<>();

            //Create a detective with an added ticket move to a different location
            //Add the move to a list, so we can move game states
            for (Piece.Detective detective : detectives) {
                Optional<Integer> detectiveLocationOptional = board.getDetectiveLocation(detective);
                if (detectiveLocationOptional.isEmpty())
                    throw new IllegalStateException("Cannot find location for detective");
                int detectiveLocation = detectiveLocationOptional.get();

                Pair<Player, Move> revisedPlayer =
                        GameStateFactory.getRevisedPlayer(board, detective, detectiveLocation, possibleLocation, detectivePlayers);
                System.out.println("Revised player: "+ revisedPlayer.left());
                detectivePlayers.add(revisedPlayer.left());
                moves.add(revisedPlayer.right());

            }

            for (Piece piece : piecesRemaining) {
//          Is always detective since it is detective's turn.
                Optional<Integer> optionalLocation = board.getDetectiveLocation((Piece.Detective) piece);
                if (optionalLocation.isEmpty()) throw new IllegalStateException("Cannot get location for detective");

                Player player = new Player(
                        piece,
                        BoardHelpers.getTicketsForPlayer(board, piece),
                        optionalLocation.get()
                );
                detectivePlayers.add(player);
            }


            Pair<Player, Move> revisedMrX =
                    GameStateFactory.getRevisedPlayer(board, MRX, possibleLocation, possibleLocation, detectivePlayers);

            List<Move> updatedMoves = new LinkedList<>(moves);
            updatedMoves.add(revisedMrX.right());
            Player mrX = revisedMrX.left();

            Board.GameState newGameState = myGameStateFactory.build(
                    BoardHelpers.generateGameSetup(board),
                    mrX,
                    ImmutableList.copyOf(detectivePlayers)
            );

            updatedMoves = updatedMoves
                    .stream()
                    .sorted(Comparator.comparingInt(o -> Color.decode(o.commencedBy().webColour()).getRGB()))
                    .toList();

            int moveNumber = 0;
            //Update the game state to advance with the move
            for (Move updatedMove : updatedMoves) {
                moveNumber++;
                System.out.println("Move number: " + moveNumber);
                if (moveNumber == 1) {
                    System.out.println(String.format("Player: %s, Move: %s", mrX, updatedMove));
                } else {
                    System.out.println(String.format("Player: %s, Move: %s", detectivePlayers.get(moveNumber - 1), updatedMove));
                }
                System.out.print(String.format("Locations: %s", BoardHelpers.getDetectiveLocations(newGameState)));
                newGameState = newGameState.advance(updatedMove);
                System.out.println("Updated player to new move");
            }
            possibleGameStates.add(newGameState);
        }

        return possibleGameStates;
    }
}
