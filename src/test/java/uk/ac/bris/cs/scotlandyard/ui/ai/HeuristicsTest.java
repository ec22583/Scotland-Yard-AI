package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
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

        //Verify it is turn 0 (no one has moved yet
        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_FIRST_TWO_ROUNDS);

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

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_FIRST_TWO_ROUNDS);

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

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_FIRST_TWO_ROUNDS);

        //if it is a secret move then it should pass the filter because it is turn 3
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(true);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);

    }

    /** test Move Filtering - RemoveFromFirstTwoRounds round 3
     * Round 1 -> 3: Don't remove the secret ticket whose second part is a secret move as that was taken at turn 3.
     * */
    @Test public void testMoveFilteringDontRemoveRoundThreeDoubleMove(){
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

        //Create a double Move
        Move secretDoubleMoveCase1 = new Move.DoubleMove
                (MRX, 36, Ticket.TAXI, 35, Ticket.SECRET, 36);
        Move secretDoubleMoveCase2 = new Move.DoubleMove
                (MRX, 36, Ticket.SECRET, 35, Ticket.TAXI, 36);
        Move taxiDoubleMove = new Move.DoubleMove
                (MRX, 36, Ticket.TAXI, 35, Ticket.TAXI, 36);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_FIRST_TWO_ROUNDS);

        //Double ticket has a secret move in the second part. It should pass the filter as the secret move
        // was taken in round 3
        assertThat(testFilter.execute(secretDoubleMoveCase1, detectiveGameState))
                .isEqualTo(true);

        //Double ticket has a secret move in the first part. It should fail the filter as the secret move
        // was taken in round 2
        assertThat(testFilter.execute(secretDoubleMoveCase2, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiDoubleMove, detectiveGameState))
                .isEqualTo(true);
    }

    /** test Move Filtering - RemoveFromRevealingRound round 3
     * Filter out any secret tickets at round 3 because it is revealing round
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

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        assertThat(detectiveGameState
                .getSetup()
                .moves
                .get(gameState.getMrXTravelLog().size())
                .equals(true))
                .isEqualTo(true);

        //Create relevant filter to test (Filter secret move at a reavealing round)
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_REVEALING_ROUND);

        //if it is a secret move then it should fail the filter because it is turn 3 (revealing round)
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);

    }

    /**
     * Test out move filtering - RemoveFromRevealingRound round 1 -> 3
     * Here Mr X will play a double move with the second part being a secret move. The filter should filter this out.
     * */
    @Test public void testRevealingRoundFilterWorksDoubleMoveCase1(){
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

        Move secretDoubleMove = new Move.DoubleMove
                (MRX, 36, Ticket.TAXI, 35, Ticket.SECRET, 36);
        Move taxiDoubleMove = new Move.DoubleMove
                (MRX, 36, Ticket.TAXI, 35, Ticket.TAXI, 36);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Turn 2 is not a revealing round so this shouldn't be true
        assertThat(detectiveGameState
                .getSetup()
                .moves
                .get(gameState.getMrXTravelLog().size())
                .equals(true))
                .isEqualTo(false);

        //Create relevant filter to test (Filter secret move at a reavealing round)
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_REVEALING_ROUND);

        //Since the second part of the move is a secret ticket it should fail the filter
        assertThat(testFilter.execute(secretDoubleMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiDoubleMove, detectiveGameState))
                .isEqualTo(true);
    }

    /**
     * Test out move filtering - RemoveFromRevealingRound round 2 -> 4
     * Here Mr X will play a double move with the first part being a secret move. The filter should filter this out.
     * */
    @Test public void testRevealingRoundFilterWorksDoubleMoveCase2(){
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

        Move secretDoubleMove = new Move.DoubleMove
                (MRX, 35, Ticket.SECRET, 36, Ticket.TAXI, 35);
        Move taxiDoubleMove = new Move.DoubleMove
                (MRX, 35, Ticket.TAXI, 36, Ticket.TAXI, 35);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Turn 3 is a revealing round so this should be true
        assertThat(detectiveGameState
                .getSetup()
                .moves
                .get(gameState.getMrXTravelLog().size())
                .equals(true))
                .isEqualTo(true);

        //Create relevant filter to test (Filter secret move at a revealing round)
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_REVEALING_ROUND);

        //Since the first part of the double move is a secret ticket it should fail the filter
        assertThat(testFilter.execute(secretDoubleMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiDoubleMove, detectiveGameState))
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
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.REMOVE_FROM_REVEALING_ROUND);

        //if it is a secret move then it should pass the filter (it isn't a revealing round)
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(true);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);
    }

    /** test Move Filtering - AllPossibleLocationsHaveTaxis round 1
     * filter out any secret tickets at round 1 because all adjacent locations can be accessed via taxi
     * */
    @Test public void testAllPossibleLocationsHaveTaxisRound1Case1() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        //Location 36 adjacent locations (35,37,49) can all be accessed via taxi
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
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.ALL_POSSIBLE_LOCATIONS_HAVE_TAXIS);

        //if it is a secret move then it should fail the filter (all adjacent locations can be accesed via taxi)
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(false);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);
    }

    /** test Move Filtering - AllPossibleLocationsHaveTaxis round 1
     * don't filter out any secret tickets at round 1 because not all adjacent locations can be accessed via taxi
     * */
    @Test public void testAllPossibleLocationsHaveTaxisRound1Case2() {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 194),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        //Location 36 adjacent locations (35,37,49) can all be accessed via taxi
        Move secretMove = new Move.SingleMove(MRX, 194, Ticket.SECRET, 157);
        Move taxiMove = new Move.SingleMove(MRX, 194, Ticket.TAXI, 157);
        gameState = gameState.advance(secretMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Verify it is turn 1
        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Create relevant filter to test
        Heuristics.MoveFiltering heuristic =
                new Heuristics.MoveFiltering();
        Heuristics.MoveFiltering.FilterStrategy testFilter = (heuristic.getFilterStrategies())
                .get(heuristic.ALL_POSSIBLE_LOCATIONS_HAVE_TAXIS);


        ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph =
                gameState.getSetup().graph;
        Set<EndpointPair<Integer>> secretMoveEdges =
                gameState.getSetup().graph.incidentEdges(secretMove.source());

        //Taken from the heuristic
        //157 -> 194 is a ferry ticket so this should evaluate as false
        assertThat(
                secretMoveEdges
                        .stream()
                        .parallel()
                        .allMatch(e -> {
                            Optional<ImmutableSet<Transport>> optionalTransports = graph.edgeValue(e);
                            //System.out.println(optionalTransports);
                            if (optionalTransports.isEmpty())
                                throw new IllegalArgumentException("Cannot find edge on graph");
                            return (optionalTransports.get().contains(ScotlandYard.Transport.TAXI));
                        }))
                .isEqualTo(false);

        //if it is a secret move then it should pass the filter
        assertThat(testFilter.execute(secretMove, detectiveGameState))
                .isEqualTo(true);

        //if it isn't a secret move then it should pass the filter
        assertThat(testFilter.execute(taxiMove, detectiveGameState))
                .isEqualTo(true);
    }
}
