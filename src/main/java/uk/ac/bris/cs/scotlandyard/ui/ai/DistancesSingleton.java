package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class DistancesSingleton {

    static private DistancesSingleton instance = null;

    //Variables wrapped inside the singleton
	private int[][] distancesArray;

    private DistancesSingleton(){
        distancesArray = readDistances();
    }

    /**
     * reads distances from the distances.txt file and put it into the distances property for other instances to use
     * @return the distance table
     * @throws IllegalStateException if the distances.txt file is not in the correct format
	 * @throws IOException if the distances.txt can't be read
     * */
    private static int[][] readDistances () {
		try {
			String file = Resources.toString(
							Resources.getResource("distances.txt"),
							StandardCharsets.UTF_8
					);
			int dimensions = ScotlandYard.standardGraph().nodes().size();

			String[] distanceStrings = file.split("\n");


			int[][] distancesArray = new int[dimensions][dimensions];
			for (int i = 0; i < distanceStrings.length; i++) {
				String distanceString = distanceStrings[i];

				List<Integer> distance = Arrays.stream(distanceString.split(","))
						.map(Integer::valueOf).toList();
				if (distance.size() != 3) throw new IllegalStateException(
						"distances.txt not in correct format: Must be comma separated list of 3 items\n" +
								"0. Starting location.\n" +
								"1. End location.\n" +
								"2. Distance"
				);


				distancesArray[distance.get(0) - 1][distance.get(1) - 1] = distance.get(2);

			}
			return distancesArray;
		}
		catch (IOException e) {
			System.err.println("Cannot read from distances.txt");
			System.exit(1);
			return null;
		}
	}

	/**
	 * @param l1 location 1
	 * @param l2 location 2
	 * @return distances between location 1 and location 2
	 * */
	public int get (int l1, int l2) {
		return this.distancesArray[l1 - 1][l2 - 1];
	}

    static public DistancesSingleton getInstance(){
        if (DistancesSingleton.instance == null){
            instance = new DistancesSingleton();
        }
        return instance;
    }

}
