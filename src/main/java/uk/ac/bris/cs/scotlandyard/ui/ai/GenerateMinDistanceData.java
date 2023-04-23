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

    private LocationCategorization.MinDistanceData data;
    private final DistancesSingleton distances;
    private PossibleLocations possibleLocations;

    public GenerateMinDistanceData () throws IOException {
        this.distances = DistancesSingleton.getInstance();

        File file = new File("min-distance-data.txt");
        if (file.exists()) {
            this.data = LocationCategorization.MinDistanceData.buildFromContinuedFile(file);
        }
        else {
            this.data = LocationCategorization.MinDistanceData.buildInitial();
        }


        PossibleLocationsFactory possibleLocationsFactory = new PossibleLocationsFactory();
        this.possibleLocations = possibleLocationsFactory.buildInitialLocations();
    }

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

    @Override
    public void onGameTurn (AIGameState aiGameState, Move move) {
        this.possibleLocations = this.possibleLocations.updateLocations(aiGameState);
        System.out.println("------------------------------------------------------------");
        System.out.println("Possible locations: " + this.possibleLocations.getLocations());
        System.out.println("Detective locations: " + aiGameState.getDetectiveLocations());
        System.out.println("Mr X Location: " + aiGameState.getMrXLocation());
        System.out.println("Move: " + move);
        System.out.println("Turn number: " + aiGameState.getMrXTravelLog().size());
        System.out.println("------------------------------------------------------------");

        if (move.commencedBy().equals(Piece.MrX.MRX)) {
            List<Integer> detectiveLocations = aiGameState.getDetectiveLocations();
            int mrXLocation = move.source();

            int realMinDistance = detectiveLocations
                    .stream()
                    .map(l -> this.distances.get(mrXLocation, l))
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElseThrow();

            this.data.addHit(MinDistance.getCategoryFromDistance(realMinDistance));

//          Updates map with possible locations.
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

            minDistanceSet.forEach(d -> this.data.addMiss(d));

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

            System.out.println("Running main");


            while (true) {
                System.out.println("Running game");
                gameSimulator.runGame();
                System.gc();
            }

        } catch (IOException e) {
            System.err.println("Couldn't read/write to dataset.txt");
            System.exit(1);
        }
    }
}
