package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

// Separate AI Entity Behaviour
public class MrXAI implements AI {
    private AIGameStateFactory aiGameStateFactory;
    private PossibleLocations possibleLocations;
    private PossibleLocationsFactory possibleLocationsFactory;

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

        Node mctsTree = new Node(
                gameState,
                this.possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient()
        );

        AI.runThreads(mctsTree, timeoutPair);

        return mctsTree.getBestChild().getPreviousMove();
    }
}
