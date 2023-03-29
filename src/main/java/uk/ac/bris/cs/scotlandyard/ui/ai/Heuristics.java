package uk.ac.bris.cs.scotlandyard.ui.ai;


import uk.ac.bris.cs.scotlandyard.model.Piece;

//Wrapper class for all Heuristics (interfaces)
public class Heuristics {

    /**
     * Create a pre-computed distance table from every node to every node
     * such that this table will have a lookup time of O(1) time.
     *
     * This distance table is naive and does not consider positioning of other
     * players blocking the path of a shortest path.
     * Used in localization categorization and E-greedy playouts.
     * */
    public void createDijkstraDistanceTable(){

    }

    /**
     * Apply rules to improve MrX's use of secret tickets (Ticket economy)
     * */
    public class MoveFiltering {

        //TODO: I have designed a framework for Move filtering. We just need to
        //implement all of them.
        // We also need to implement all appropriate return types

        private void removeFromFirstTwoRounds(){
        }

        private void removeFromRevealingRound(){

        }

        private void allPossibleLocationsHaveTaxis(){

        }

        /**
         * Method used to apply secret ticket filtering.
         * */
        public void filterSecretTickets(){
            this.removeFromFirstTwoRounds();
            this.removeFromRevealingRound();
            this.allPossibleLocationsHaveTaxis();
        }
    }

    /**
     * Categorize locations based on distance (max, avg) from detective and types of connections
     * a location has
     * */
    public class LocalizationCategorization {
        //TODO: I have designed a framework for Localization Categorization. We need to implement the return types
        //and the actual implementation.

        private void categorizeMinimumDistance(){

        }

        private void categorizeAverageDistance(){

        }

        private void categorizeStationType(){

        }

        //Main method to apply all categorization techniques
        public void applyCategories(){

        }

    }

    /**
     * Epsilon greedy playout
     * */
    public class EGreedyPlayouts {
        //TODO: I have designed a framework for E-Greedy playouts.

        //For MrX
        public void maximizeDetectiveDistance (){

        }

        public void minimizeMrXDistance(){

        }
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
         * @param currentPiece the root piece as used in the node data structure
         * @param value the piece to compare against the root piece
         * @returns a double giving the evaluation score for that win
         * */
        public static double calculateValue (Piece currentPiece, Piece value) {
            //if MrX or a matching detective piece
            if (currentPiece.equals(value)) return 1;
            else if (currentPiece.isDetective() && value.isDetective()) {
                return 0.625;
            }
            else return 0;
        }
    }

}
