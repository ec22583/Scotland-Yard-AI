package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class MyAi implements Ai {
	private MrXAI mrXAI;
	private DetectiveAI detectiveAI;

	public void onStart() {
		this.mrXAI = new MrXAI();
		this.detectiveAI = new DetectiveAI(DistancesSingleton.getInstance());
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
