package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

//0.0 is the neutral evaluation bar

// Separate AI Entity Behaviour
public class MrXAI {
    MyGameStateFactory myGameStateFactory;
    GameSetup gameSetup;
    Score score;

    Board.GameState currentState;

//  Stores possible game states and their scores and the move to get there.
    List<PossibleGameState> futureStates;

    MrXAI () {
        this.myGameStateFactory = new MyGameStateFactory();
        this.futureStates = new LinkedList<>();
        this.score = new Score();
//        this.score.addWeight(new ConnectionWeight(2));
        this.score.addWeight(new DistanceWeights.SumDistance());
        this.score.addWeight(new DistanceWeights.MinimumDistance(2));
        this.gameSetup = null;
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board) {
//      Store copy of game setup but with mrX always revealed.

        this.futureStates.clear();
        this.currentState = this.generateGameState(board);

        //Iterate each of the moves and add each one to the tree
        for (Move move : this.currentState.getAvailableMoves()) {
            Board.GameState newGameState = currentState.advance(move);
            this.futureStates.add(
                    new PossibleGameState(
                            newGameState,
                            score.calculateScore(newGameState),
                            move
                    )
            );
        }

        PossibleGameState bestGameState = this.futureStates.get(0) ;
        //move in the inner pair is the move from the previous state that gets you to this state
        for (PossibleGameState state : this.futureStates) {
            if (state.evaluation() > bestGameState.evaluation()) {
                bestGameState = state;
            }
        }

        return bestGameState.move();
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
        List<Boolean> moveSetup = new LinkedList<>();
//      Ensures that MrX is always visible to AI.
        for (int i = 0; i < (board.getSetup().moves.size() - board.getMrXTravelLog().size()); i++) {
            moveSetup.add(true);
        }
        gameSetup = new GameSetup(
                board.getSetup().graph,
                ImmutableList.copyOf(moveSetup)
        );

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
