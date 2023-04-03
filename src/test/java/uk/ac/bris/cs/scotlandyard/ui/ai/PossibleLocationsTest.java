package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;
import uk.ac.bris.cs.scotlandyard.model.Player;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public class PossibleLocationsTest extends AITestBase{
    @Test public void testInitialPossibleLocationsCorrect () {
        PossibleLocations possibleLocations =
                getPossibleLocationsFactory().buildInitialLocations();

//      Only contains possible starting locations for Mr X.
        assertThat(possibleLocations.getLocations())
                .hasSameElementsAs(MRX_LOCATIONS);

//      Starting turn is always 0.
        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);
    }

    @Test public void testSingleMovePossiblePositionsCorrect () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));

        possibleLocations = possibleLocations.updateLocations(gameState);

//      Check that it contains the actual location
//      and not the original locations of Mr X.
        assertThat(possibleLocations.getLocations())
                .contains(36, 22, 48, 65)
                .doesNotContainAnyElementsOf(MRX_LOCATIONS)

//              Cannot contain locations of detectives.
                .doesNotContain(50);
    }

        @Test public void testDoubleMovePossiblePositionsCorrect () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(
                new Move.DoubleMove(MRX, 35, Ticket.TAXI, 22, Ticket.TAXI, 23)
        );
        possibleLocations = possibleLocations.updateLocations(gameState);

//      Check that it contains the actual location
//      and all the original locations of Mr X (double move allows Mr X to move back to start).
        assertThat(possibleLocations.getLocations())
                .doesNotContain(50)
                .contains(23)
                .containsAll(MRX_LOCATIONS)

//              Cannot contain locations of detectives.
                .doesNotContain(50);
    }

    @Test public void testDetectivePositionFiltered () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);

        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        gameState = gameState.advance(new Move.SingleMove(MRX, 36, Ticket.TAXI, 37));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getLocations())
//              Check that actual location and test location contained.
                .contains(37, 66);

        gameState = gameState.advance(new Move.SingleMove(RED, 49, Ticket.TAXI, 66));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getLocations())
//              Check that actual location contained.
                .contains(37)
//              Check that detective's position is removed.
                .doesNotContain(66);
    }

    @Test public void testKnownLocationReducesPossibleLocations () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);

        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        gameState = gameState.advance(new Move.SingleMove(MRX, 36, Ticket.TAXI, 37));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getLocations())
//              Has multiple locations before location known.
                .hasSizeGreaterThan(1);

        gameState = gameState.advance(new Move.SingleMove(RED, 49, Ticket.TAXI, 66));

        gameState = gameState.advance(new Move.SingleMove(MRX, 37, Ticket.TAXI, 50));
        possibleLocations = possibleLocations.updateLocations(gameState);

//      Only contains single correct element.
        assertThat(possibleLocations.getLocations())
                .hasSize(1)
                .contains(50);
    }

    @Test public void testSingleMoveUpdatesTurnCount () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getTurn())
                .isEqualTo(1);
    }

        @Test public void testDoubleMoveUpdatesTurnCount () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);

        gameState = gameState.advance(
                new Move.DoubleMove(MRX, 35, Ticket.TAXI, 22, Ticket.TAXI, 23)
        );
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getTurn())
                .isEqualTo(2);
    }

    @Test public void testOnlyEdgesWhichMatchTicketAdded () {
                PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 22));
        possibleLocations = possibleLocations.updateLocations(gameState);

        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        gameState = gameState.advance(new Move.SingleMove(MRX, 22, Ticket.TAXI, 35));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getLocations())
//              Actual location, as well as all accessible taxi locations from 22.
                .contains(35, 34, 23, 11)
//              Would require BUS taken but TAXI ticket used.
                .doesNotContain(65, 3);
    }

    @Test public void testSecretTicketAddsAllAdjacentEdges () {
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        assertThat(possibleLocations.getTurn())
                .isEqualTo(0);

        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 22));
        possibleLocations = possibleLocations.updateLocations(gameState);

        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        gameState = gameState.advance(new Move.SingleMove(MRX, 22, Ticket.SECRET, 35));
        possibleLocations = possibleLocations.updateLocations(gameState);

        assertThat(possibleLocations.getLocations())
//              Actual location, as well as all accessible locations from 22.
                .contains(35, 34, 23, 11, 65, 3);
    }
}
