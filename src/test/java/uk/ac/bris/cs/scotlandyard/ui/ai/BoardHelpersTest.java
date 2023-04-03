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

//Verify that all BoardHelper functions work as intended
public class BoardHelpersTest extends AITestBase {

    //Simple identity check
    @Test public void verifyStartLocations(){
        assertThat(BoardHelpers.START_LOCATIONS)
                .contains(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172);
    }

    //Check correct retrieval
    @Test public void verifyGetDetectives(){
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(
                        new Player(RED, defaultDetectiveTickets(), 50),
                        new Player(BLUE, defaultDetectiveTickets(), 48),
                        new Player(GREEN, defaultDetectiveTickets(), 49),
                        new Player(WHITE, defaultDetectiveTickets(), 51),
                        new Player(YELLOW, defaultDetectiveTickets(), 52)
                )
        );

        List<Player> playerPieces = BoardHelpers.getDetectives(gameState);
        assertThat(playerPieces)
                .contains(
                        new Player(RED, defaultDetectiveTickets(), 50),
                        new Player(BLUE, defaultDetectiveTickets(), 48),
                        new Player(GREEN, defaultDetectiveTickets(), 49),
                        new Player(WHITE, defaultDetectiveTickets(), 51),
                        new Player(YELLOW, defaultDetectiveTickets(), 52)
                );
    }

    //Verify correct retrieval of a player after a single move
    @Test public void verifyGetDetectivesAfterMove() {
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(
                        new Player(RED, defaultDetectiveTickets(), 50)
                )
        );
        gameState = gameState.advance(new Move.SingleMove(MRX, 35, Ticket.TAXI, 36));
        gameState = gameState.advance(new Move.SingleMove(RED, 50, Ticket.TAXI, 49));

        //Red's ticket after his move. He consumed a taxi ticket (11 -> 10)
        ImmutableMap<ScotlandYard.Ticket, Integer> RedTickets =
                ImmutableMap.of(
                        ScotlandYard.Ticket.TAXI, 10,
                        ScotlandYard.Ticket.BUS, 8,
                        ScotlandYard.Ticket.UNDERGROUND, 4,
                        ScotlandYard.Ticket.SECRET, 0,
                        ScotlandYard.Ticket.DOUBLE, 0);

        List<Player> playerPieces = BoardHelpers.getDetectives(gameState);
        assertThat(playerPieces)
                .contains(
                        new Player(RED, RedTickets, 49)
                );
    }

    @Test public void verifyDetectiveLocations(){
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(
                        new Player(RED, defaultDetectiveTickets(), 50),
                        new Player(BLUE, defaultDetectiveTickets(), 48),
                        new Player(GREEN, defaultDetectiveTickets(), 49),
                        new Player(WHITE, defaultDetectiveTickets(), 51),
                        new Player(YELLOW, defaultDetectiveTickets(), 52)
                )
        );

        List<Integer> detectiveLocations = BoardHelpers.getDetectiveLocations(gameState);

        //Verify it does contain detective locations
        assertThat(detectiveLocations)
                .contains(48,49,50,51,52);
    }
}
