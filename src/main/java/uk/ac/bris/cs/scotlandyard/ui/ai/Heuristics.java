package uk.ac.bris.cs.scotlandyard.ui.ai;


import uk.ac.bris.cs.scotlandyard.model.Piece;

//Wrapper class for all Heuristics (interfaces)
public class Heuristics {

    /**
     * Apply rules to improve MrX's use of secret tickets (Ticket economy)
     * */
    public class MoveFiltering {

    }

    /**
     * Categorize locations based on distance (max, avg) from detective and types of connections
     * a location has
     * */
    public class LocalizationCategorization {

    }

    /**
     * Epsilon greedy playout
     * */
    public class EGreedyPlayouts {

    }

    /**
     * Set a value of r to ensure optimization between co-operation and independent
     * hunting of MR X
     * */
    public class CoalitionReduction {

        /**
         * Application of Coalition Reduction. If root piece is detective but not the value piece then
         * give only 62.5% weighting on the value.
         *
         * @param rootPiece the root piece as used in the node data structure
         * @param value the piece to compare against the root piece
         * @returns a double giving the evaluation score for that win
         * */
        public static double calculateValue (Piece rootPiece, Piece value) {
            if (rootPiece.equals(value)) return 1;
            else if (rootPiece.isDetective() && value.isDetective()) {
                return 0.625;
            }
            else return 0;
        }
    }

}
