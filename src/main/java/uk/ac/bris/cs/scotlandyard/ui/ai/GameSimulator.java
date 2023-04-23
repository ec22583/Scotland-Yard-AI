package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Allows games to be simulated for the generation of statistical data about the AI.
 */
public class GameSimulator {
    private final List<GameObserver> gameObservers;
    private final GameSetup gameSetup;
    private final AIGameStateFactory aiGameStateFactory;
    private final Pair<Long, TimeUnit> timeoutPair;

    /**
     * Allows data to be collected about simulated game.
     */
    public interface GameObserver {

        /**
         * Called when simulated game begins
         */
        default void onGameStart() {}

        /**
         * Called during a turn in a game
         * @param aiGameState Game state from before move is carried out.
         * @param move move to be used on aiGameState.
         */
        default void onGameTurn(AIGameState aiGameState, Move move) {}

        /**
         * Called when a game has finished (win state is reached)
         * @param aiGameState Winning game state.
         * */
        default void onGameWin(AIGameState aiGameState) {}
    }

    /**
     * @param gameSetup Game setup for simulated games.
     * @param aiGameStateFactory Factory to generate game states for the AI
     * @param timeoutPair Time limit per turn.
     * */
    public GameSimulator (GameSetup gameSetup, AIGameStateFactory aiGameStateFactory, Pair<Long, TimeUnit> timeoutPair) {

        this.timeoutPair = timeoutPair;
        this.aiGameStateFactory = aiGameStateFactory;
        this.gameObservers = new LinkedList<>();
        this.gameSetup = gameSetup;
    }

    /**
     * Adds an observer to the game simulator
     * @param gameObserver game observer to feed into the function
     * @throws IllegalArgumentException if there already exists an observer
     * */
    public void registerObserver (@Nonnull GameObserver gameObserver) {
        Objects.requireNonNull(gameObserver);

        if (this.gameObservers.contains(gameObserver)) throw new IllegalArgumentException("Observer already registered");

        this.gameObservers.add(gameObserver);
    }

    /**
     * Remove an observer to the game simulator
     * @param gameObserver game observer to deregister into the function
     * @throws IllegalArgumentException if there does not exist an observer
     * */
    public void deregisterObserver (@Nonnull GameObserver gameObserver) {
        Objects.requireNonNull(gameObserver);

        if (!this.gameObservers.contains(gameObserver)) throw new IllegalArgumentException("Observer not registered");

        this.gameObservers.remove(gameObserver);
    }

    /**
     * Start simulation of game
     * */
    public void runGame () {
        MyAi ai = new MyAi();
        ai.onStart();
        this.gameObservers.forEach(GameObserver::onGameStart);

        Piece.Detective[] detectiveColors = Piece.Detective.values();
        ImmutableList<Integer> detectiveLocations = ScotlandYard.generateDetectiveLocations(new Random().nextInt(), 5);

        Player mrX = new Player(
            Piece.MrX.MRX,
            ScotlandYard.defaultMrXTickets(),
            ScotlandYard.generateMrXLocation(new Random().nextInt())
        );

        List<Player> detectives = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            detectives.add(new Player(
                    detectiveColors[i],
                    ScotlandYard.defaultDetectiveTickets(),
                    detectiveLocations.get(i)
            ));
        }

        AIGameState aiGameState = aiGameStateFactory.build(
                gameSetup,
                mrX,
                ImmutableList.copyOf(detectives)
        );

        //Run until winner
        while (aiGameState.getWinner().isEmpty()) {
            Move move = ai.pickMove(aiGameState, this.timeoutPair);
            AIGameState finalAiGameState = aiGameState;
            this.gameObservers.forEach(o -> o.onGameTurn(finalAiGameState, move));
            aiGameState = aiGameState.advance(move);
        }

        AIGameState finalAiGameState1 = aiGameState;
        this.gameObservers.forEach(o -> o.onGameWin(finalAiGameState1));
        ai.onTerminate();
    }
}
