package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public class MyAi implements Ai {
	private MrXAI mrXAI;
	private DetectiveAI detectiveAI; //for later TODO
	public void onStart() {
		this.mrXAI = new MrXAI();
		this.detectiveAI = new DetectiveAI();
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
			return mrXAI.generateBestMove(board, timeoutPair);
		}
//		Run detectives' turn
		else {
			return detectiveAI.generateBestMove(board, timeoutPair);
		}
	}

	public void onTerminate() {}
}
