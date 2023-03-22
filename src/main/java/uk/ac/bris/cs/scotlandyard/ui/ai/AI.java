package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.TimeUnit;

public interface AI {
    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);
}
