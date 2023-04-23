package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.List;

/**
 * All created visitors for {@link Move}
 */
interface MoveVisitors {
    /**
     * Gets a list of Tickets which are used for a move.
     */
    class TicketVisitor implements Move.Visitor<List<ScotlandYard.Ticket>> {
        @Override
        public List<ScotlandYard.Ticket> visit(Move.SingleMove move) {
            return ImmutableList.of(move.ticket);
        }

        @Override
        public List<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
            return ImmutableList.of(move.ticket1, move.ticket2);
        }
    }

    /**
     * Gets final destination for move.
     */
    class DestinationVisitor implements Move.Visitor<Integer>{
        Integer destination;

        @Override
        public Integer visit(Move.SingleMove move) {
            destination = move.destination;
            return destination;
        }

        @Override
        public Integer visit(Move.DoubleMove move) {
            destination = move.destination2;
            return destination;
        }
    }

    /**
     * Splits a {@link Move} into separate {@link uk.ac.bris.cs.scotlandyard.model.Move.SingleMove} objects.
     */
    class SingleMoveVisitor implements Move.Visitor<List<Move.SingleMove>> {
        List<Move.SingleMove> moves = new ArrayList<>();

        @Override
        public List<Move.SingleMove> visit(Move.SingleMove move) {
            moves.add(move);
            return moves;
        }

        /**
         * Decomposes double move into two separate single moves.
         * @param move Double move to split.
         * @return Two single moves in a list.
         */
        @Override
        public List<Move.SingleMove> visit(Move.DoubleMove move) {
            Move.SingleMove move1 = new Move.SingleMove(move.commencedBy(), move.source(), move.ticket1, move.destination1);
            Move.SingleMove move2 = new Move.SingleMove(move.commencedBy(), move.destination1, move.ticket2, move.destination2);
            moves.add(move1);
            moves.add(move2);
            return moves;
        }
    }
}
