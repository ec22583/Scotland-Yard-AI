package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class MyAi implements Ai {
	private MrXAI mrXAI;
	private DetectiveAI detectiveAI;

	public void onStart() {
		ExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

//      Ensures that executor service closed when the program is shut down.
//      (based on https://stackoverflow.com/questions/5824049/running-a-method-when-closing-the-program)
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));

		this.mrXAI = new MrXAI(executorService);
		this.detectiveAI = new DetectiveAI(executorService, DistancesSingleton.getInstance());
	}

	/**
	 * @return Awesome name :D
	 */
	@Nonnull @Override
	public String name() {
		return "Pair<AI<MCTS>, Pair<ThomasParr, WallaceDegamo>>";
	}

	@Nonnull @Override
	public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		List<Move> availableMoves = board.getAvailableMoves().asList();
		Move bestMove;

//		Create game state from board
//		Check if current player is MrX.
		if (availableMoves.get(0).commencedBy().isMrX()) {
			bestMove = mrXAI.generateBestMove(board, timeoutPair);
		}
//		Run detectives' turn
		else {
			bestMove = detectiveAI.generateBestMove(board, timeoutPair);
		}

//		Calls garbage collector for more aggressive garbage collection
		System.gc();

		return bestMove;
	}

	public void onTerminate() {}
}
