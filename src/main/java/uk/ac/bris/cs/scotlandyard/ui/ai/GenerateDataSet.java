package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GenerateDataSet {
    AIGameStateFactory aiGameStateFactory;
    MyAi myAi;
    FileWriter output;
    GameSetup gameSetup;
    Pair<Long, TimeUnit> timeoutPair;

    public GenerateDataSet (String filename, Pair<Long, TimeUnit> timeoutPair) throws IOException {
        this.myAi = new MyAi();
        this.myAi.onStart();
        this.aiGameStateFactory = new AIGameStateFactory();
        this.timeoutPair = timeoutPair;
        File file = new File(filename);

        if (file.exists()) {
            this.output = new FileWriter(file, true);
        } else {
            this.output = new FileWriter(file, true);
            this.output.append(
                    "mrxlocation," +
                    "detectivelocation1," +
                    "detectivelocation2," +
                    "detectivelocation3," +
                    "detectivelocation4," +
                    "detectivelocation5," +
                    "detective1taxi," +
                    "detective1bus," +
                    "detective1train," +
                    "detective2taxi," +
                    "detective2bus," +
                    "detective2train," +
                    "detective3taxi," +
                    "detective3bus," +
                    "detective3train," +
                    "detective4taxi," +
                    "detective4bus," +
                    "detective4train," +
                    "detective5taxi," +
                    "detective5bus," +
                    "detective5train," +
                    "mrxtaxi," +
                    "mrxbus," +
                    "mrxtrain," +
                    "mrxdouble," +
                    "mrxsecret," +
                    "turnsleft," +
                    "newmrxlocation" +
                    "\n"
            );
            this.output.flush();
        }

        this.gameSetup =  new GameSetup(
                ScotlandYard.standardGraph(),
                ScotlandYard.STANDARD24MOVES
        );

//      Ensures that file is closed when the program is shut down.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                output.close();
                System.out.println("Closed dataset.txt");
            } catch (IOException e) {
                System.err.println("Couldn't close dataset.txt");
            }
        }));
    }

    private void runGame (int gameCounter) {
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

        int mrXTurnCounter = 0;
        List<Integer> lastState = new ArrayList<>(20);

        while (aiGameState.getWinner().isEmpty()) {
            if (aiGameState.getAvailableMoves().asList().get(0).commencedBy().equals(Piece.MrX.MRX)) {
                mrXTurnCounter++;
                lastState.addAll(aiGameState.getGameStateList());
                Move move = myAi.pickMove(aiGameState, this.timeoutPair);
                aiGameState = aiGameState.advance(move);
                lastState.add(move.accept(new MoveVisitors.DestinationVisitor()));
                try {
                    String line = lastState.stream().map(String::valueOf).collect(Collectors.joining(","));
                    this.output.append(line + "\n");
                    this.output.flush();
                    lastState.clear();
                } catch (IOException e) {
                    System.err.println("Couldn't add to file");
                    System.exit(1);
                }
            } else {
                Move move = myAi.pickMove(aiGameState, this.timeoutPair);
                aiGameState = aiGameState.advance(move);
            }

        }

        System.out.println(String.format("Game %s finished. Turns added this game: %s.", gameCounter, mrXTurnCounter));

    }

    public static void main (String[] args) {
        try {
            GenerateDataSet generateDataSet = new GenerateDataSet("dataset.txt", new Pair<>((long) 15, TimeUnit.SECONDS));
            int gameCounter = 0;
            while (true) {
                gameCounter++;
                generateDataSet.runGame(gameCounter);
                System.gc();
            }
        } catch (IOException e) {
            System.err.println("Couldn't read/write to data set file.");
            System.exit(1);
        }
    }
}
