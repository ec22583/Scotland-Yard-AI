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

    Board.GameState currentState;

//  Stores possible game states and their scores and the move to get there.
    List<PossibleGameState> futureStates;

    //Wrapper for our GameState values
    final class PossibleGameState {
        final private Board.GameState gameState;
        final private double evaluation;

//      Move to get you to this state from the previous state.
        final private Move move;

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
        this.futureStates = new LinkedList<>();
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board) {
        this.currentState = this.generateGameState(board);

        //Iterate each of the moves and add each one to the tree
        for (Move move : this.currentState.getAvailableMoves()) {
            Board.GameState newGameState = currentState.advance(move);
            this.futureStates.add(new PossibleGameState(newGameState, 0.0, move));
        }

        PossibleGameState bestGameState = this.futureStates.get(0) ;
        //move in the inner pair is the move from the previous state that gets you to this state
        for (PossibleGameState state : this.futureStates) {
            if (state.getEvaluation() > bestGameState.getEvaluation()) {
                bestGameState = state;
            }
        }

        return bestGameState.getMove();
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
