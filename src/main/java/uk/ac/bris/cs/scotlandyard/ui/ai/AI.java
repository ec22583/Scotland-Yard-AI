package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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


    /**
     * @param mctsTree the MCTS tree in which the agent controls
     * @param timeoutPair amount of time the thread lasts for
     * */
    static void runThreads(Node mctsTree, Pair<Long, TimeUnit> timeoutPair){
        try {
            ThreadController controller = new ThreadController();

            //cores in the cpu
            final int threadsUsed = Runtime.getRuntime().availableProcessors();
            //Runs for allocated turn time - 100ms
            final long milliseconds = timeoutPair.right().toMillis(timeoutPair.left()) - 100;

            System.out.println("Number of \'available processors\' - 2: " + threadsUsed);

            ExecutorService executorService = Executors.newFixedThreadPool(threadsUsed);

            //Submit MCTS tasks per thread
            for (int i = 0; i < threadsUsed; i++) {
                executorService.submit(new MCTS(mctsTree, controller));
            }



            executorService.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);

            System.out.format("Total number of iterations: %s\n", controller.getIterations());
//      Not expected to receive an interrupt on current thread so just return early.
        } catch (InterruptedException e) {}
    }

    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);
}
