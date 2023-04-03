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

    @Test public void test1stDetectiveTurnCorrect () {
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
                .contains(36)
                .doesNotContainAnyElementsOf(MRX_LOCATIONS)

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
}
