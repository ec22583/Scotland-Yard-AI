package uk.ac.bris.cs.scotlandyard.ui.ai;

import io.atlassian.fugue.Pair;
import org.glassfish.grizzly.streams.BufferedInput;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GenerateDataSet implements GameSimulator.GameObserver {
    private final FileWriter output;

    /**
     * @throws IOException can't close the data set file or find the file
     * */
    public GenerateDataSet (String filename) throws IOException {
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

//      Ensures that file is closed when the program is shut down.
//      (based on https://stackoverflow.com/questions/5824049/running-a-method-when-closing-the-program)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                output.close();
            } catch (IOException e) {
                System.err.println("Couldn't close dataset.txt");
            }
        }));
    }

    /**
     * @param  aiGameState AI game state to modify
     * @param move move to use.
     * */
    @Override
    public void onGameTurn (AIGameState aiGameState, Move move) {

        //If AI is mr X
        if (aiGameState.getAvailableMoves().asList().get(0).commencedBy().equals(Piece.MrX.MRX)) {


            if (aiGameState.advance(move).getWinner().isEmpty()) {
                List<Integer> lastState = new ArrayList<>(25);
                lastState.addAll(aiGameState.getGameStateList());
                lastState.add(move.accept(new MoveVisitors.DestinationVisitor()));

                try {
                    String line = lastState.stream().map(String::valueOf).collect(Collectors.joining(","));
                    this.output.append(line).append("\n");
                    this.output.flush();
                } catch (IOException e) {
                    System.err.println("Couldn't add to file");
                    System.exit(1);
                }
            }

        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void main (String[] args) {
        try {
            Scanner terminal = new Scanner(System.in);
            GenerateDataSet generateDataSet = new GenerateDataSet("dataset.txt");
            AIGameStateFactory aiGameStateFactory = new AIGameStateFactory();

            GameSimulator gameSimulator = new GameSimulator(
                    new GameSetup(
                        ScotlandYard.standardGraph(),
                        ScotlandYard.STANDARD24MOVES
                    ),
                    aiGameStateFactory,
                    new Pair<>((long) 15, TimeUnit.SECONDS)
            );

            gameSimulator.registerObserver(generateDataSet);


//          Allows program to be closed with ^d
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
