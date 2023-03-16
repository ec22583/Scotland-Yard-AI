package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;


//Our own Tree data structure to store our game states
public class TreeGameState {
    private Board.GameState gameState;
    private int totalPlays;
    private int wins;
    private Move previousMove;

    public TreeGameState (Board.GameState gameState, Move previousMove) {
        this.gameState = gameState;
        this.previousMove = previousMove;
        this.wins = 0;
        this.totalPlays = 0;
    }

    public TreeGameState (Board.GameState gameState) {
        this.gameState = gameState;
        this.previousMove = null;
        this.wins = 0;
        this.totalPlays = 0;
    }

    public void addWin () {
        this.wins += 1;
        this.totalPlays += 1;
    }

    public void addLoss () {
        this.totalPlays += 1;
    }

    public int getWins () {
        return this.wins;
    }

    public int getTotalPlays () {
        return this.totalPlays;
    }

    public Board.GameState getGameState () {
        return this.gameState;
    }

    public Move getPreviousMove () {
        return this.previousMove;
    }

    public boolean equals (TreeGameState other) {
        return this.gameState.equals(other.getGameState());
    }
}
