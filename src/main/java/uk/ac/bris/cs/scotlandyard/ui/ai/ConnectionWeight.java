package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;

public class ConnectionWeight implements Score.Weight {
    int multiplier;

    public ConnectionWeight() {
        this.multiplier = 1;
    }

    public ConnectionWeight(int multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public double calculateWeight(Board.GameState gameState) {
        ImmutableList<LogEntry> logEntries = gameState.getMrXTravelLog();
        int location = logEntries.get(logEntries.size() - 1).location().get();

        return gameState.getSetup().graph.outDegree(location);
    }
}
