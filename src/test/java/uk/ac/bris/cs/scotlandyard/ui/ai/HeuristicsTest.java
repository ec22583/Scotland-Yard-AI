package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
public class HeuristicsTest extends AITestBase {
    //Need to make an AI game state

    //GameSetup gameSetup = gameState.getSetup();


    /** test Move Filtering - RemoveFromFirstTwoRounds round 1
     * Filter out any secret tickets at round 1
     * */
    @Test public void testMoveFilteringRemoveRoundOne() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        Move secretMove = new Move.SingleMove(MRX, 35, Ticket.SECRET, 36);
        Move taxiMove = new Move.SingleMove(MRX, 35, Ticket.TAXI, 36);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0);

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies()).get(0);

        //if it is a secret move then it should fail the filter
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);


    }

    /** test Move Filtering - RemoveFromFirstTwoRounds round 2
     * Filter out any secret tickets at round 2
     * */
    @Test public void testMoveFilteringRemoveRoundTwo() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);
        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        Move secretMove = new Move.SingleMove(MRX, 36, Ticket.SECRET, 35);
        Move taxiMove = new Move.SingleMove(MRX, 36, Ticket.TAXI, 35);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 2
        assertThat(possibleLocations.getTurn())
                .isEqualTo(2);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0);

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies()).get(0);

        //if it is a secret move then it should fail the filter
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);


    }

    /** test Move Filtering - RemoveFromFirstTwoRounds round 3
     * Don't filter out any secret tickets at round 3 (using the RemoveFromFirstTwoRounds filter strategy)
     * */
    @Test public void testMoveFilteringDontRemoveRoundThree() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);
        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        gameState = gameState.advance(new Move.SingleMove(MRX, 36, Ticket.TAXI, 35));
        possibleLocations = possibleLocations.updateLocations(gameState);
        gameState = gameState.advance(new Move.SingleMove(RED, 49, Ticket.TAXI, 50));

        //Verify it is turn 2
        assertThat(possibleLocations.getTurn())
                .isEqualTo(2);

        Move secretMove = new Move.SingleMove(MRX, 35, Ticket.SECRET, 36);
        Move taxiMove = new Move.SingleMove(MRX, 35, Ticket.TAXI, 36);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 3
        assertThat(possibleLocations.getTurn())
                .isEqualTo(3);


        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0);

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies()).get(0);

        //if it is a secret move then it should pass the filter because it is turn 3
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(true);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);



    }

    /** test Move Filtering - RemoveFromRevealingRound round 3
     * Filter out any secret tickets at round 3 because it si revealing round
     * */
    @Test public void testReavlingRoundFilterWorksRound3() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);
        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        gameState = gameState.advance(new Move.SingleMove(MRX, 36, Ticket.TAXI, 35));
        possibleLocations = possibleLocations.updateLocations(gameState);
        gameState = gameState.advance(new Move.SingleMove(RED, 49, Ticket.TAXI, 50));

        //Verify it is turn 2
        assertThat(possibleLocations.getTurn())
                .isEqualTo(2);

        Move secretMove = new Move.SingleMove(MRX, 35, Ticket.SECRET, 36);
        Move taxiMove = new Move.SingleMove(MRX, 35, Ticket.TAXI, 36);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 3
        assertThat(possibleLocations.getTurn())
                .isEqualTo(3);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0);

        assertThat(detectiveGameState
                .getSetup()
                .moves
                .get(gameState.getMrXTravelLog().size() - 1)
                .equals(true))
                .isEqualTo(true);

        //Create relevant filter to test (Filter secret move at a reavealing round)
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies()).get(1);

        //if it is a secret move then it should fail the filter because it is turn 3 (revealing round)
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);

    }

    /** test Move Filtering - RemoveFromRevealingRound round 1
     * Don't filter out any secret tickets at round 1 because it isn't a revealing round
     * */
    @Test public void testReavlingRoundFilterWorksRound1() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        Move secretMove = new Move.SingleMove(MRX, 35, Ticket.SECRET, 36);
        Move taxiMove = new Move.SingleMove(MRX, 35, Ticket.TAXI, 36);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0);

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies()).get(1);

        //if it is a secret move then it should pass the filter (it isn't a revealing round)
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(true);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);
    }
}
