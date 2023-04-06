package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class MyAi implements Ai {
	private MrXAI mrXAI;
	private DetectiveAI detectiveAI;

	public void onStart() {
		this.mrXAI = new MrXAI();
		this.detectiveAI = new DetectiveAI();
//		this.readDistances();
		PrecalculateDistances.main();
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

	private ImmutableTable<Integer, Integer, Integer> readDistances () {
		try {
			String file = Resources.toString(
							Resources.getResource("distances.txt"),
							StandardCharsets.UTF_8
					);

			String[] distanceStrings = file.split(",");

			ImmutableTable.Builder<Integer, Integer, Integer> builder = ImmutableTable.builder();
			for (int i = 0; i < distanceStrings.length; i++) {
				Integer rowKey = i / 200;
				Integer columnKey = i % 200;
				builder.put(rowKey, columnKey, Integer.valueOf(distanceStrings[i]));
			}
			ImmutableTable<Integer, Integer, Integer> distances = builder.build();
			System.out.println();
		} catch (IOException e) {
			System.err.println("Cannot read from distances.txt");
			System.exit(1);
		}

		return null;
	}

	public void onTerminate() {}
}
