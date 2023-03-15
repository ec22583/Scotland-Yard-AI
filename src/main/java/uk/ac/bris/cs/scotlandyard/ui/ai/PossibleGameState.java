package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

/**
 * @param move Move to get you to this state from the previous state.
 */
//Wrapper for our GameState values
record PossibleGameState(Board.GameState gameState, double evaluation, Move move) {
}
