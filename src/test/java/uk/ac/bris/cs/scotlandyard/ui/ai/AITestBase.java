package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.defaultDetectiveTickets;

public class AITestBase {
    private static MyGameStateFactory gameStateFactory;
    private static PossibleLocationsFactory possibleLocationsFactory;

    private static AIGameStateFactory aiGameStateFactory;
    private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;

    private static DistancesSingleton distancesSingleton;

    @BeforeClass
    public static void setUp () {
        possibleLocationsFactory = new PossibleLocationsFactory();
        gameStateFactory = new MyGameStateFactory();
        aiGameStateFactory = new AIGameStateFactory();
        distancesSingleton = DistancesSingleton.getInstance();

        try {
			defaultGraph = readGraph(Resources.toString(Resources.getResource(
					"graph.txt"),
					StandardCharsets.UTF_8));
		} catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }

    /**
     * Used for standardized testing.
     * AI Game state is a detective game state.
     * The game state used is turn 0 with MrX + one detective. (Mr X to move)
     * */
    public static Node constructStandardRootNode(){
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

    /**
     * Used for standardized testing.
     * AI Game state is a detective game state.
     * The game state used is turn 1 with MrX + one detective. (Detective RED to move)
     * */
    public static Node constructStandardChildNode(){
        PossibleLocations possibleLocations = getPossibleLocationsFactory().buildInitialLocations();
        Board.GameState gameState = MyGameStateFactory.a(
                standard24MoveSetup(),
                new Player(MRX, defaultMrXTickets(), 35),
                ImmutableList.of(new Player(RED, defaultDetectiveTickets(), 50))
        );

        Move.SingleMove previousMove = new Move.SingleMove(MRX, 35, Ticket.TAXI, 36);
        gameState = gameState.advance(previousMove);
        possibleLocations = possibleLocations.updateLocations(gameState);

        //Get first AI detective's game state.
        //it doesn't matter which detective is being fed into the heuristic, results should stay consistent
        AIGameState detectiveGameState = aiGameStateFactory()
                .buildDetectiveGameStates(gameState, possibleLocations).get(0).left();

        //Standard root node
        Node rootNode = constructStandardRootNode();

        Node newNode =
                new Node(
                        detectiveGameState,
                        rootNode,
                        rootNode,
                        previousMove,
                        possibleLocations,
                        new Heuristics.MoveFiltering(),
                        new Heuristics.CoalitionReduction(),
                        new Heuristics.ExplorationCoefficient()
                );

        return newNode;
    }

    public static DistancesSingleton getDistancesSingleton() {return distancesSingleton;}

    public static MyGameStateFactory getGameStateFactory () {
        return gameStateFactory;
    }

    public static PossibleLocationsFactory getPossibleLocationsFactory () {
        return possibleLocationsFactory;
    }

    public static AIGameStateFactory aiGameStateFactory () {
        return aiGameStateFactory;
    }
    /**
	 * @return the default graph used in the actual game
	 */
	@Nonnull
    static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> standardGraph() {
        return defaultGraph;
    }

    @Nonnull static GameSetup standard24MoveSetup() {
        return new GameSetup(defaultGraph, STANDARD24MOVES);
    }

}
