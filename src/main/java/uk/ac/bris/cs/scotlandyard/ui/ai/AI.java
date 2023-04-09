package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.TimeUnit;

//Not to be confused with "Ai" which is implemented by MyAi
public interface AI {

    /**
     * Helper function to sleep the current thread for the allotted time.
     * @param timeoutPair The length of time to sleep the thread for - 250 ms.
     */
    private static void sleepThread(Pair<Long, TimeUnit> timeoutPair){
        try {
//          Sleeps the program for the (time - 250 ms).
            Thread.sleep(TimeUnit.MILLISECONDS.convert(timeoutPair.left(), timeoutPair.right()) - 250);
        } catch (InterruptedException e) {
//          Handles if an interrupt is thrown towards the current method while asleep.
//          Leaving this blank is fine since continuing the program if interrupt is expected behaviour.
        }
    }

    static void runThreads(Node mctsTree, Pair<Long, TimeUnit> timeoutPair){
        Thread m = Thread.currentThread();
        MCTS mcts = new MCTS(mctsTree, m);

//      Starts thread that runs the Monte Carlo Tree Search.
        mcts.start();

//      Sleeps program to let MCTS algorithm run
        AI.sleepThread(timeoutPair);

//      Interrupts the algorithm which causes it to stop testing paths.
        if (mcts.isAlive()) mcts.interrupt();
    }

    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);
}
