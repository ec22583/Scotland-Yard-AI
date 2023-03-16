package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// Separate AI Entity Behaviour
public class MrXAI {
    private MyGameStateFactory myGameStateFactory;
    private GameSetup gameSetup;
    private Board.GameState gameState;
    private MCTS mcts;

    public MrXAI () {
        this.myGameStateFactory = new MyGameStateFactory();
        this.mcts = new MCTS();
    }

    //Average wins: Wins / total plays. Helper function to generateBestMove
    private double getAverageScore(TreeGameState treeGameState){
        return  Double.valueOf(treeGameState.getWins())
                / Double.valueOf(treeGameState.getTotalPlays());
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board) {
        this.gameSetup = board.getSetup();
        gameState = this.generateGameState(board);

        this.mcts.updateGameState(gameState);
        Tree<TreeGameState> gameStateTree = this.mcts.run();

        List<TreeGameState> nextTreeGameStates = gameStateTree.getChildValues();

//      Assume first child is best.
//      Calculates the average score of the path.
        double averageScore = this.getAverageScore(nextTreeGameStates.get(0));
        Move bestMove = nextTreeGameStates.get(0).getPreviousMove();

        for (TreeGameState treeGameState : nextTreeGameStates) {
            double newTreeGameStateAvgScore = this.getAverageScore(treeGameState);
            if (newTreeGameStateAvgScore > averageScore) {
                averageScore = newTreeGameStateAvgScore;
                bestMove = treeGameState.getPreviousMove();
            }
        }

        return bestMove;
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
        Set<Piece> detectivePieces = new HashSet<>(board
                .getPlayers()
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
//                          Piece must be cast to a Detective. Not an issue since mrx filtered out earlier
//                            (For type safety). .get() fine as piece always is a detective.
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
