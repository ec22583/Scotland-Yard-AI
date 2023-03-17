package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.sql.Time;
import java.util.*;
import java.lang.Thread;
import java.util.concurrent.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

// Separate AI Entity Behaviour
public class MrXAI {
    private MyGameStateFactory myGameStateFactory;
    private GameSetup gameSetup;
    Tree<TreeGameState> gameStateTree;
    Board.GameState gameState;

    public MrXAI () {
        this.myGameStateFactory = new MyGameStateFactory();
    }

    //Average wins: Wins / total plays. Helper function to generateBestMove
    private double getAverageScore(TreeGameState treeGameState){
        return  Double.valueOf(treeGameState.getWins())
                / Double.valueOf(treeGameState.getTotalPlays());
    }

    //Evaluate the Best move from a Game tree
    public Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair) {
        this.gameSetup = board.getSetup();
        this.gameState = this.generateGameState(board);
        this.gameStateTree = new Tree<>(new TreeGameState(gameState));

        MCTS mcts = new MCTS(gameStateTree);
//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();

        try {
//          Sleeps the program for the time - 500 milliseconds.
            Thread.sleep(TimeUnit.MILLISECONDS.convert(timeoutPair.left(), timeoutPair.right()) - 500);
        } catch (InterruptedException e) { // Handles if an interrupt is thrown towards the current method while asleep.
            Thread.currentThread().interrupt();
            return null;
        }

        mcts.interrupt(); // Interrupts the algorithm which causes it to stop testing paths.

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

    //Helper function
    private int getLocation(Board board){
        int location = board
                .getAvailableMoves()
                .asList()
                .get(0)
                .source();

        return location;
    }

    //Helper function. Get detectives
    private ImmutableList<Player> getDetectives (Board board){

        List<Player> detectives = new LinkedList<>(board
                .getPlayers()
                .stream()
                .filter(p -> p.isDetective())
                .map(piece -> new Player(
                        piece,
                        // Generates tickets for piece.
                MrXAI.getTicketsForPlayer(board, piece),
                        //  Piece must be cast to a Detective. Not an issue since mrx filtered out earlier
                        // (For type safety). .get() fine as piece always is a detective.
                board.getDetectiveLocation((Piece.Detective) piece).get()
                ))
                .toList()
        );

        return ImmutableList.copyOf(detectives);
    }

    private Board.GameState generateGameState (Board board) {
        List<Boolean> moveSetup = new LinkedList<>();

//      Ensures that MrX is always visible to AI. (Himself)
        for (int i = 0; i < (board.getSetup().moves.size() - board.getMrXTravelLog().size()); i++) {
            moveSetup.add(true);
        }
        gameSetup = new GameSetup(
                board.getSetup().graph,
                ImmutableList.copyOf(moveSetup)
        );

        int location = getLocation(board);
        Player mrX = new Player(MRX, MrXAI.getTicketsForPlayer(board, MRX) , location);
        ImmutableList<Player> detectives = getDetectives(board);

        //Construct a new game state with parameters above
        Board.GameState gameState = myGameStateFactory.build(
                gameSetup,
                mrX,
                detectives
        );

        return gameState;
    }

}
