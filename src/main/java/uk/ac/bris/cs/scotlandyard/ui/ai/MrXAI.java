package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

//0.0 is the neutral evaluation bar

// Separate AI Entity Behaviour
public class MrXAI {
    MyGameStateFactory myGameStateFactory;
    Tree<Pair<Board.GameState, Pair<Move, Double>>> stateTree; //Gamestate and Evaluation bar for that respective gamestate

    //Wrapper for our GameState values
    final class PossibleGameState {
        final private Board.GameState gameState;
        final private double evaluation;
        final private Move move; //move to get you to this state from the previous state

        public PossibleGameState(Board.GameState gameState, double evaluation, Move move) {
            this.gameState = gameState;
            this.evaluation = evaluation;
            this.move = move;
        }

        public Board.GameState getGameState() {
            return gameState;
        }

        public double getEvaluation() {
            return evaluation;
        }

        public Move getMove() {
            return move;
        }
    }

    MrXAI () {
        this.myGameStateFactory = new MyGameStateFactory();
        this.stateTree = new Tree<>();
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board) {
        Board.GameState currentState = this.generateGameState(board);
        stateTree.getRoot().setValue(new Pair<>(currentState, new Pair<>(null, 0.0)));

        //Iterate each of the moves and add each one to the tree
        for (Move move : board.getAvailableMoves()) {
            Board.GameState newGameState = currentState.advance(move);
            stateTree.getRoot().addChild(new Pair<>(newGameState, new Pair<>(move, 0.0)));
        }

        double maxScore = 0;
        Move bestMove;
        //move in the inner pair is the move from the previous state that gets you to this state
        for (Pair<Board.GameState, Pair<Move, Double>> node : stateTree) {
            if (!node.equals(currentState)) {
                if (maxScore < node.right().right()) {
                    bestMove = node.right().left();
                    maxScore = node.right().right();
                }
            }
        }

        // returns a random move, replace with your own implementation
        var moves = board.getAvailableMoves().asList();
        return moves.get(new Random().nextInt(moves.size()));
    }

    //Helper method
    static private ImmutableMap<ScotlandYard.Ticket, Integer> getTicketsForPlayer (Board board, Piece piece) {

//      Get a TicketBoard of tickets
        Board.TicketBoard tickets =
                board.getPlayerTickets(board
                        .getPlayers()
                        .stream()
                        .parallel()
                        .filter(p -> p.equals(piece))
                        .findAny()
                        .get()
                ).get();

        //      Generates map of ticket values from current TicketBoard state.
        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();
        for (ScotlandYard.Ticket ticketType : ScotlandYard.Ticket.values()) {
            ticketMap.put(ticketType, tickets.getCount(ticketType));
        }

        return ImmutableMap.copyOf(ticketMap);
    }

    private Board.GameState generateGameState (Board board) {
        GameSetup gameSetup = board.getSetup();

        int location = board
                .getAvailableMoves()
                .asList()
                .get(0)
                .source();

        Player mrX = new Player(MRX, MrXAI.getTicketsForPlayer(board, MRX) , location);

        //Get detective pieces
        Set<Piece> detectivePieces = new HashSet<>(board.getPlayers()
                .stream()
                .parallel()
                .filter(p -> p.isDetective())
                .toList()
        );

//      All detectives
        List<Player> detectives = detectivePieces
                .stream()
                .parallel()
                .map((piece) -> new Player(
                            piece,

//                          Generates tickets for piece.
                            MrXAI.getTicketsForPlayer(board, piece),

//                          Piece must be cast to a Detective. (For type safety)
                            board.getDetectiveLocation((Piece.Detective) piece).get()
                    )
                )
                .toList();

        Board.GameState gameState = myGameStateFactory.build(
                gameSetup,
                mrX,
                ImmutableList.copyOf(detectives)
        );

        return gameState;
    }


}
