package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;

import java.util.List;

public class Score {
    List<Weight> weightFunctions;

    public interface Weight {
        public double calculateWeight (Board.GameState gameState);
    }

    public double calculateScore (Board.GameState gameState) {
        return weightFunctions
                .stream()
                .map(f -> f.calculateWeight(gameState))
                .mapToDouble(Double::valueOf)
                .sum();
    }
}
