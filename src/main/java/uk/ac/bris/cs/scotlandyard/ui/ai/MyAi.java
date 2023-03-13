package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class MyAi implements Ai {
	private MrXAI mrXAI;
//	private detectivesAi; //for later TODO
	public void onStart() {
		mrXAI = new MrXAI();
	}

	@Nonnull @Override
	public String name() {
		return "Name me!";
	}

	@Nonnull @Override
	public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		List<Move> availableMoves = board.getAvailableMoves().asList();

//		Create game state from board
//		Check if current player is MrX.
		if (availableMoves.get(0).commencedBy().isMrX()) {
			return mrXAI.generateBestMove(board);
		}
//		Run detectives' turn
		else {
			// returns a random move, replace with your own implementation
			var moves = board.getAvailableMoves().asList();
			return moves.get(new Random().nextInt(moves.size()));
		}
	}

	public void onTerminate() {}
}
