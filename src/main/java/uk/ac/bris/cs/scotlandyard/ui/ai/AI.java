package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.*;

//Not to be confused with "Ai" which is implemented by MyAi
public interface AI {
    /**
     * @param mctsTree the MCTS tree in which the agent uses
     * @param timeoutPair amount of time the thread lasts for
     * */
    static void runThreads(Node mctsTree, Pair<Long, TimeUnit> timeoutPair){
        try {
            ThreadController controller = new ThreadController();
            MCTS mcts = new MCTS(mctsTree, controller);

            //cores in the cpu
            final int threadsUsed = Runtime.getRuntime().availableProcessors();
            //Runs for allocated turn time - 100ms
            final long milliseconds = timeoutPair.right().toMillis(timeoutPair.left()) - 100;

            System.out.println("Number of \'available processors\': " + threadsUsed);

            ExecutorService executorService = Executors.newFixedThreadPool(threadsUsed);

            //Submit MCTS tasks per thread
            //This is safe to do because mcts isn't stateful and therefore immune to data races
            for (int i = 0; i < 30000; i++){
                executorService.submit(mcts);
            }

//          Shuts down the service but allows already queued tasks to run.
            executorService.shutdown();

//          Waits either for the service to shut down or for the time limit.
            executorService.awaitTermination(milliseconds, TimeUnit.MILLISECONDS);
            executorService.shutdownNow();
            

            System.out.format("Total number of iterations: %s\n", controller.getIterations());
//      Not expected to receive an interrupt on current thread so just return early.
        }
        catch (InterruptedException e) {
            System.out.println("Current thread interrupted while sleeping");
        }
    }

    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);
}
