package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

interface MoveVisitors {
    //  Returns all tickets used in a move.
    public class TicketVisitor implements Move.Visitor<Set<ScotlandYard.Ticket>> {
        Set<ScotlandYard.Ticket> tickets = new HashSet<ScotlandYard.Ticket>();

        @Override
        public Set<ScotlandYard.Ticket> visit(Move.SingleMove move) {
            tickets.add(move.ticket);
            return tickets;
        }

        @Override
        public Set<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
            tickets.add(move.ticket1);
            tickets.add(move.ticket2);
            return tickets;
        }
    }

    //  Returns the final destination of a move.
    public class DestinationVisitor implements Move.Visitor<Integer>{
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

    //  Returns the move as SingleMoves.
    public class SingleMoveVisitor implements Move.Visitor<List<Move.SingleMove>> {
        List<Move.SingleMove> moves = new ArrayList<>();

        @Override
        public List<Move.SingleMove> visit(Move.SingleMove move) {
            moves.add(move);
            return moves;
        }

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
