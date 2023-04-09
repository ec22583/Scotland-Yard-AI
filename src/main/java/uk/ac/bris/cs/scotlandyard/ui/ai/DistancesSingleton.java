package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class DistancesSingleton {

    static private DistancesSingleton instance = null;

    //Variables wrapped inside the singleton
    private Table<Integer, Integer, Integer> distances;

    private DistancesSingleton(){
        distances = readDistances();
    }

    /**
     * reads distances from the distances.txt file and put it into the distances property for other instances to use
     * @return the distance table
     * @throws IllegalStateException if the distances.txt file is not in the correct format
     * */
    private static Table<Integer, Integer, Integer> readDistances () {
		try {
			String file = Resources.toString(
							Resources.getResource("distances.txt"),
							StandardCharsets.UTF_8
					);

			String[] distanceStrings = file.split("\n");

			ImmutableTable.Builder<Integer, Integer, Integer> builder = ImmutableTable.builder();
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
				builder.put(distance.get(0), distance.get(1), distance.get(2));
			}
			ImmutableTable<Integer, Integer, Integer> distances = builder.build();
			return distances;
		} catch (IOException e) {
			System.err.println("Cannot read from distances.txt");
			System.exit(1);
			return null;
		}
	}

    public Table<Integer, Integer, Integer> getDistances(){
        return distances;
    }

    static public DistancesSingleton getInstance(){
        if (DistancesSingleton.instance == null){
            instance = new DistancesSingleton();
        }
        return instance;
    }

}
