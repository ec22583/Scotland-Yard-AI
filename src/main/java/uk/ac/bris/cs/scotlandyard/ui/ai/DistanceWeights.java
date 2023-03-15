package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

public interface DistanceWeights {
    class SumDistance implements Score.Weight {
        int multiplier;

        public SumDistance() {
            this.multiplier = 1;
        }

        public SumDistance(int multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public double calculateWeight(Board.GameState gameState) {
            Piece piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

            Dijkstra dijkstra = new Dijkstra(gameState, piece);
//          Get the smallest distance to each detective.
            return gameState.getPlayers()
                    .stream()
                    .parallel()
                    .filter(p -> p.isDetective())
                    .map(p -> dijkstra.getSmallestDistance(
                                        gameState.getDetectiveLocation((Piece.Detective) p).get()
                                )
                    )
                    .mapToInt(Integer::valueOf)
                    .sum();
        }
    }

    class MinimumDistance implements Score.Weight {
        int multiplier;

        public MinimumDistance() {
            this.multiplier = 1;
        }

        public MinimumDistance(int multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public double calculateWeight(Board.GameState gameState) {
            Piece piece = gameState.getAvailableMoves().asList().get(0).commencedBy();

            Dijkstra dijkstra = new Dijkstra(gameState, piece);
//          Get the smallest distance to each detective.
            return gameState.getPlayers()
                    .stream()
                    .parallel()
                    .filter(p -> p.isDetective())
                    .map(p -> dijkstra.getSmallestDistance(
                                    gameState.getDetectiveLocation((Piece.Detective) p).get()
                            )
                    )
                    .mapToInt(Integer::valueOf)
                    .min()
                    .getAsInt();
        }
    }
}
