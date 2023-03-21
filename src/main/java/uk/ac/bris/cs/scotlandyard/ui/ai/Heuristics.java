package uk.ac.bris.cs.scotlandyard.ui.ai;


//Wrapper class for all Heuristics (interfaces)
public class Heuristics {

    /**
     * Apply rules to improve MrX's use of secret tickets (Ticket economy)
     * */
    public interface MoveFiltering {

    }

    /**
     * Categorize locations based on distance (max, avg) from detective and types of connections
     * a location has
     * */
    public interface LocalizationCategorization {

    }

    /**
     * Epsilon greedy playout
     * */
    public interface EGreedyPlayouts {

    }

    /**
     * Set a value of r to ensure optimization between co-operation and independent
     * hunting of MR X
     * */
    public interface CoalitionReduction {

    }

}
