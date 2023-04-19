package uk.ac.bris.cs.scotlandyard.ui.ai;

/**
 * Delegates to the actual UI main
 */
public class Main {
	public static void main(String[] args) {
		if (args.length == 1 && args[0].equals("--generate-distances")) {
			PrecalculateDistances.main();
		} else {
			uk.ac.bris.cs.scotlandyard.Main.main(args);
		}
	}
}
