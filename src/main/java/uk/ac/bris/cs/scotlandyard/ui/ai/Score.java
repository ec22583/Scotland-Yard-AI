package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.LinkedList;
import java.util.List;

public class Score {
    List<Weight> weights;

    public interface Weight {
        int multiplier = 1;

        default public int getMultiplier() {
            return multiplier;
        }

        public double calculateWeight (Board.GameState gameState);
    }

    public Score () {
        this.weights = new LinkedList<>();
    }

    public Score (List<Weight> weights) {
        this.weights = new LinkedList<>(weights);
    }

    public double calculateScore (Board.GameState gameState) {
        return this.weights
                .stream()
                .map(f -> f.calculateWeight(gameState) * f.getMultiplier())
                .mapToDouble(Double::valueOf)
                .sum();
    }

    public void addWeight(Weight weight) {
        this.weights.add(weight);
    }

    public void removeWeight(Weight weight) {
        this.weights.remove(weight);
    }

    public  void updateWeights() {

    }
}
