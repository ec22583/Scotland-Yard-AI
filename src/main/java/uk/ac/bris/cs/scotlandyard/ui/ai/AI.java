package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.TimeUnit;

//Not to be confused with "Ai" which is implemented by MyAi
public interface AI {

    /**
     * run algorithm threads (ALlow potential for concurrency)
     * */
    void runThreads(Pair<Long, TimeUnit> timeoutPair);

    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);
}
