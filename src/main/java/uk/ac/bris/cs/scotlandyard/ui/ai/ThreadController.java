package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used to stop execution of MCTS threads after certain length of time.
 */
public class ThreadController {
    private boolean run;

    private AtomicInteger iterations;

    public ThreadController() {
        this.run = true;
        this.iterations = new AtomicInteger();
    }

    /**
     * Counts an iteration of the algorithm.
     */
    public void incrementIteration() {
        this.iterations.incrementAndGet();
    }

    /**
     * Ends execution of threads by setting boolean to false
     */
    public void end() {
        this.run = false;
    }

    public int getIterations () {
        return this.iterations.get();
    }

    public boolean getRun () {
        return this.run;
    }
}
