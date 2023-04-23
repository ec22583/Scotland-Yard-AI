package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DistancesSingleton {
    static private DistancesSingleton instance;
	final private int[][] distancesArray;

    private DistancesSingleton(){
        distancesArray = readDistances();
    }

    /**
     * Reads distances from the distances.txt file into class for use by any instance.
     * @return the distance table
     * */
    @SuppressWarnings("UnstableApiUsage")
	private static int[][] readDistances () {
		try {
			String file = Resources.toString(
							Resources.getResource("distances.txt"),
							StandardCharsets.UTF_8
					);
			int dimensions = ScotlandYard.standardGraph().nodes().size();

//			Splits on any new line character(s)
			String[] distanceStrings = file.split("\\R");

			// Create a 199 x 199 2D array as the distance table
			int[][] distancesArray = new int[dimensions][dimensions];

			for (String distanceString : distanceStrings) {
				int[] distance = Arrays
						.stream(distanceString.split(","))
						.mapToInt(Integer::valueOf)
						.toArray();
				if (distance.length != 3) throw new IOException(
						"distances.txt not in correct format: Must be comma separated list of 3 items\n" +
								"0. Starting location.\n" +
								"1. End location.\n" +
								"2. Distance"
				);

				distancesArray[distance[0] - 1][distance[1] - 1] = distance[2];

			}
			return distancesArray;
		}
		catch (IOException e) {
			System.err.println("Cannot read from distances.txt: " + e.getMessage());
			System.exit(1);
			return null;
		}
		catch (NumberFormatException e) {
			System.err.println("Error reading number from distances.txt");
			System.exit(1);
			return null;
		}
	}

	/**
	 * @param l1 location 1
	 * @param l2 location 2
	 * @return distances between location 1 and location 2
	 * @throws IllegalArgumentException arguments given are out of bounds (must be between 1-199 inclusive)
	 * */
	public int get (int l1, int l2) {
		if ((l1 < 1) || (l2 < 1) || (l1 > 199) || (l2 > 199)) throw new IllegalArgumentException("Locations only between 1 and 199");

		return this.distancesArray[l1 - 1][l2 - 1];
	}

	/**
	 * Used to access distances in code.
	 * @return Instance of {@link DistancesSingleton}
	 */
	@Nonnull
    static public DistancesSingleton getInstance(){
        if (DistancesSingleton.instance == null){
            instance = new DistancesSingleton();
        }
        return instance;
    }

}
