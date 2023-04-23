package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.Heuristics.LocationCategorization.MinDistance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.ui.ai.Heuristics.*;

public class GenerateMinDistanceData implements GameSimulator.GameObserver {

    private final LocationCategorization.MinDistanceData data;
    private final DistancesSingleton distances;
    private final PossibleLocationsFactory possibleLocationsFactory;
    private PossibleLocations possibleLocations;

    /**
     * @throws IOException if files cannot be created/read from.
     */
    public GenerateMinDistanceData () throws IOException {
        this.distances = DistancesSingleton.getInstance();

        File file = new File("min-distance-data.txt");
        if (file.exists()) {
            this.data = LocationCategorization.MinDistanceData.buildFromContinuedFile(file);
        }
        else {
            this.data = LocationCategorization.MinDistanceData.buildInitial();
        }


        this.possibleLocationsFactory = new PossibleLocationsFactory();
    }

    /**
     * Initialises {@link PossibleLocations} to initial state on game start.
     */
    @Override
    public void onGameStart () {
        this.possibleLocations = this.possibleLocationsFactory.buildInitialLocations();
    }

    /**
     * Writes the current Map of category data to 'min-distance-data.txt'
     */
    private void writeDataToFile () {
        StringBuilder outputString = new StringBuilder("category,total-hits,total-possible\n");
        for (MinDistance category : MinDistance.values()) {
            outputString.append(category.name()).append(",");

            outputString.append(this.data.getTotalHits(category)).append(",");
            outputString.append(this.data.getTotalPossible(category)).append("\n");
        }
        try (FileWriter output = new FileWriter("min-distance-data.txt", false)) {
                output.write(outputString.toString());
        } catch (IOException e) {
            System.err.println("Cannot write to 'min-distance-data.txt'");
            System.exit(1);
        }
    }

    /**
     * Processes categorization data for current turn.
     * @param aiGameState Game state from before move is carried out.
     * @param move move to be used on aiGameState.
     */
    @Override
    public void onGameTurn (AIGameState aiGameState, Move move) {
        aiGameState = aiGameState.advance(move);
        this.possibleLocations = this.possibleLocations.updateLocations(aiGameState);

//      If game not winning state and current turn Mr X's
        if (aiGameState.getWinner().isEmpty() && move.commencedBy().equals(Piece.MrX.MRX)) {
            List<Integer> detectiveLocations = aiGameState.getDetectiveLocations();
            int mrXLocation = aiGameState.getMrXLocation();

//          Actual minimum distance between Mr X and detectives.
            int realMinDistance = detectiveLocations
                    .stream()
                    .map(l -> this.distances.get(mrXLocation, l))
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElseThrow();

            this.data.addHit(MinDistance.getCategoryFromDistance(realMinDistance));

//          Updates map with all possible categories for current turn.
            Set<MinDistance> minDistanceSet = ImmutableSet.copyOf(this.possibleLocations
                    .getLocations()
                    .stream()
                    .map(l -> detectiveLocations
                                .stream()
                                .map(d -> this.distances.get(d, l))
                                .mapToInt(Integer::intValue)
                                .min()
                                .orElseThrow())
                    .map(MinDistance::getCategoryFromDistance)
                    .toList());

            minDistanceSet.forEach(this.data::addMiss);

            this.writeDataToFile();
            System.out.println("Writing update to file");
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void main (String[] args) {
        try {
            AIGameStateFactory aiGameStateFactory = new AIGameStateFactory();
            GenerateMinDistanceData generateMinDistanceData = new GenerateMinDistanceData();

            GameSimulator gameSimulator = new GameSimulator(
                    new GameSetup(
                        ScotlandYard.standardGraph(),
                        ScotlandYard.STANDARD24MOVES
                    ),
                    aiGameStateFactory,
                    new Pair<>((long) 15, TimeUnit.SECONDS)
            );

            gameSimulator.registerObserver(generateMinDistanceData);

            while (true) {
                gameSimulator.runGame();
                System.gc();
            }

        } catch (IOException e) {
            System.err.println("Couldn't read/write to dataset.txt");
            System.exit(1);
        }
    }
}
