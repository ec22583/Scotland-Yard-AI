package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// Separate AI Entity Behaviour
public class MrXAI {
    MyGameStateFactory myGameStateFactory;
    GameSetup gameSetup;

    MrXAI () {
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board) {
        return null;
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
