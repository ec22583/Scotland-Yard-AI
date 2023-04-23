package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

public class MrXAI implements PlayerAI {
    private static final long BUFFER = 200;
    private final AIGameStateFactory aiGameStateFactory;
    private PossibleLocations possibleLocations;
    private final PossibleLocationsFactory possibleLocationsFactory;

    public MrXAI () {
        this.aiGameStateFactory = new AIGameStateFactory();
        this.possibleLocationsFactory = new PossibleLocationsFactory();
    }

    /**
     * Given the game state, generate the best move for Mr X
     * @param board game state in which to evaluate the best move
     * @param timeoutPair amount of time the thread lasts for
     * */
    @Nonnull @Override
    public Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair) {
        if (this.possibleLocations == null) {
            this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
        }
        this.possibleLocations = this.possibleLocations.updateLocations(board);

        AIGameState gameState = this.aiGameStateFactory.buildMrXGameState(board);

        return PlayerAI.runMCTSForGameState(
                gameState,
                possibleLocations,
                timeoutPair,
                BUFFER
        );
    }
}
