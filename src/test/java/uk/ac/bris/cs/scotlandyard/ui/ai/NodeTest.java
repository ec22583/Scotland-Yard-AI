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

public class NodeTest extends AITestBase{

    /**
     * Used for standardized testing.
     * AI Game state is a detective game state.
     * The game state used is turn 1 with MrX + one detective. (Mr X to move)
     * */
    static public Node constructStandardRootNode(){
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();


        //Create a Root node
        Node newNode =
                new Node(
                        detectiveGameState,
                        possibleLocations,
                        new Heuristics.MoveFiltering(),
                        new Heuristics.CoalitionReduction(),
                        new Heuristics.ExplorationCoefficient()
                );
        return newNode;
    }

    @Test
    public void verifyRootConstructorWorks(){
        Node node = constructStandardRootNode();

        //Simple Identity checking
        assertThat(node.getTotalValue() == 0 ).isEqualTo(true);
        assertThat(node.getTotalPlays() == 0 ).isEqualTo(true);

        //Since this is a root node there should be no virtual loss at the start
        assertThat(node.getVirtualLoss() == 0).isEqualTo(true);
        //Since no moves has been performed therefore the piece must be MrX
        assertThat(node.getPiece().equals(MRX)).isEqualTo(true);
        //Verify this is realy a root node
        assertThat(node.getParent() == null).isEqualTo(true);
    }

    //The standard root node is just a game state in turn 1, therefore it is impossible for it to be game over.
    @Test
    public void verifyStandardRootNodeIsNotGameOver(){
        Node node = constructStandardRootNode();

        assertThat(node.isNotGameOver()).isEqualTo(true);
    }

    //static method getGameWinner is fed a AIGameState (same used in standardRootNode)
    //Since the game state is in turn one it should return Optional.empty() as the winner because it is impossible
    //to have a winner if none of the players have moved.
    @Test
    public void verifyGetGameWinner(){
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        assertThat(Node.getGameWinner(detectiveGameState).equals(Optional.empty()))
                .isEqualTo(true);
    }

    //Idea: we aren't looking for a specific winning piece, but more rather proof that there exists a winning piece
    //by checking if the return value is non null
    @Test
    public void verifySimulateGame(){
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Verify if winning piece is non-null
        assertThat(
                Node.simulateGame(detectiveGameState, possibleLocations, new Heuristics.EGreedyPlayouts()) != null)
                .isEqualTo(true);
    }

    //Given our standard root node, isFullyExpanded function should return false
    @Test
    public void verifyIsFullyExpanded(){
        Node node = constructStandardRootNode();

        assertThat(node.isFullyExpanded() == false).isEqualTo(true);
    }
}
