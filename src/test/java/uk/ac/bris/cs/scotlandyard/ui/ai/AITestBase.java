package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.MyGameStateFactory;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.STANDARD24MOVES;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class AITestBase {
    private static MyGameStateFactory gameStateFactory;
    private static PossibleLocationsFactory possibleLocationsFactory;

    private static AIGameStateFactory aiGameStateFactory;
    private static ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;

    @BeforeClass
    public static void setUp () {
        possibleLocationsFactory = new PossibleLocationsFactory();
        gameStateFactory = new MyGameStateFactory();
        aiGameStateFactory = new AIGameStateFactory();

        try {
			defaultGraph = readGraph(Resources.toString(Resources.getResource(
					"graph.txt"),
					StandardCharsets.UTF_8));
		} catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
    }

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
