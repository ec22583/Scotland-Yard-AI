package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.concurrent.*;

/**
 * Interface used to define AI for detectives and Mr X.
 * Not the same as {@link uk.ac.bris.cs.scotlandyard.model.Ai} implemented with {@link MyAi}
 */
public interface PlayerAI {
    /**
     * Agents will run using multiple threads to maximize their iterations within the time limit.
     * @param mctsTree the MCTS tree in which the agent uses
     * @param timeout Maximum time for iterations to run in milliseconds.
     * */
    static void runThreads(Node mctsTree, long timeout){
        try {
            MCTS mcts = new MCTS(mctsTree);

            //cores in the cpu
            final int threadsUsed = Runtime.getRuntime().availableProcessors();

            ExecutorService executorService = Executors.newFixedThreadPool(threadsUsed);

//          Ensures that threads are closed when the program is shut down. (Prevent lingering thread errors)
//          (based on https://stackoverflow.com/questions/5824049/running-a-method-when-closing-the-program)
            Thread shutdownThread = new Thread(executorService::shutdownNow);
            Runtime.getRuntime().addShutdownHook(shutdownThread);


            //Submit MCTS tasks per thread
            //This is safe to do because mcts isn't stateful and therefore immune to data races
            for (int i = 0; i < 50000; i++){
                executorService.submit(mcts);
            }

//          Shuts down the service but allows already queued tasks to run.
            executorService.shutdown();

//          Waits either for the service to shut down or for the time limit.
            if (!executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }

//          Stops shutdown being called on already shut down service.
            Runtime.getRuntime().removeShutdownHook(shutdownThread);

//      Not expected to receive an interrupt on current thread so just return early.
        }
        catch (InterruptedException e) {
            System.out.println("Current thread interrupted while sleeping");
        }
    }

    /**
     * Runs the MCTS algorithm from the passed in game state and returns the best move.
     * @param gameState game state to be used in MCTS algorithm
     * @param possibleLocations possible locations of Mr X
     * @param timeoutPair Maximum time allowed by game for AI to run.
     * @param BUFFER buffer used to limit the time allowed
     * */
    static Move runMCTSForGameState(
            AIGameState gameState,
            PossibleLocations possibleLocations,
            Pair<Long, TimeUnit> timeoutPair,
            long BUFFER ){

        Node mctsTree = new Node(
                gameState,
                possibleLocations,
                new Heuristics.MoveFiltering(),
                new Heuristics.CoalitionReduction(),
                new Heuristics.ExplorationCoefficient()
        );

        long timeToRun = timeoutPair.right().toMillis(timeoutPair.left()) - BUFFER;

        PlayerAI.runThreads(mctsTree, timeToRun);

        return mctsTree.getBestChild().getPreviousMove();
    }

    /**
     * Generates the best move for the player to make according to AI.
     * @param board Current game board.
     * @param timeoutPair Maximum time allowed by game for AI to run.
     * @return Best move according to MCTS algorithm.
     * */
    Move generateBestMove (Board board, Pair<Long, TimeUnit> timeoutPair);


}
