package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

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

    //Evaluate the Best move from a Game tree
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
                new Heuristics.ExplorationCoefficient(),
                new Heuristics.EGreedyPlayouts()
        );

        AI.runThreads(mctsTree, timeoutPair);

        return mctsTree.getBestChild().getPreviousMove();
    }
}
