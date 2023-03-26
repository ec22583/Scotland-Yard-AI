package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

// A factory to create game states such that the AIs can use it
// We took this from our Cw-Model coursework (closed task) and modified it for the open task
public class AIGameStateFactory {
    private final class MyGameState implements Board.GameState {

        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private List<Player> detectives;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;

        private MyGameState(final GameSetup setup,
                            final ImmutableSet<Piece> remaining,
                            final ImmutableList<LogEntry> log,
                            final Player mrX,
                            final List<Player> detectives) {

            checkParamsNotNull(setup, remaining, mrX, detectives);
            checkDetectives(detectives);

            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;

            ImmutableSet<Piece> winnerSet = MyGameState.getWinners(
                    this.detectives,
                    this.mrX,
                    this.setup,
                    this.log,
                    this.remaining);
            this.winner = ImmutableSet.copyOf(winnerSet);
            this.moves = this.getAvailableMoves();
        }

        //Helper function
        static private ImmutableSet<Piece> getWinners(List<Player> detectives,
                                                      Player mrX,
                                                      GameSetup setup,
                                                      List<LogEntry> log,
                                                      ImmutableSet<Piece> remaining){
            Set<Piece> winnerSet = new HashSet<>();

            boolean detectivesHaveMoves = false;
            for (Player detective : detectives) {
//				If a detective and MrX in same place, detective wins.
                if (detective.location() == mrX.location()) {
//
                    winnerSet.addAll(detectives
                            .stream()
                            .map(d -> d.piece())
                            .toList());
                }
//				Checks if any detectives have tickets remaining.
                if (!MyGameState.makeSingleMoves(setup, detectives, detective, detective.location()).isEmpty()) {
                    detectivesHaveMoves = true;
                }
            }

//			If detectives have no more tickets, MrX wins.
            if (!detectivesHaveMoves) {
                winnerSet.add(mrX.piece());
            }

            if (remaining.contains(mrX.piece())) { // Check if it is MrX's turn.
                if ((log.size() >= setup.moves.size())) { // Check if MrX has any more log entries to fill.
                    winnerSet.add(mrX.piece()); // //if logbook is full, then mrX wins
                }
//				Check MrX can move if it is his turn. If he can't then detectives win
                if (makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty()) {
                    winnerSet.addAll(detectives
                            .stream()
                            .map(d -> d.piece())
                            .toList());
                }
            }
            return ImmutableSet.copyOf(winnerSet);
        }

        //Helper function
        private void checkParamsNotNull(final GameSetup setup,
                                        final ImmutableSet<Piece> remaining,
                                        final Player mrX,
                                        final List<Player> detectives) {

            // Ensure params are defined.
            if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty");
            if (remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty");
            if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves are empty");

            //mrX and Detectives mustn't be null
            Objects.requireNonNull(mrX, "mrX is null");
            Objects.requireNonNull(detectives, "detectives is null");

            //Checks if an entity in detectives is null, throws an error if there exists null detective
            if (detectives
                    .stream()
                    .filter(d -> d != null)
                    .toList()
                    .isEmpty()
            ) throw new IllegalArgumentException("Detective is null");
        }

        //Helper function
        private void checkDetectives(final List<Player> detectives) {
            //Stores current locations of detectives
            Set<Integer> locations = new HashSet<>();

            //inspects all detectives
            for (Player detective : detectives) {
                // Test no illegal tickets (Double and Secret)
                if (detective.tickets().getOrDefault(ScotlandYard.Ticket.DOUBLE, 0) > 0) {
                    throw new IllegalArgumentException("Detective has double tickets");
                }
                if (detective.tickets().getOrDefault(ScotlandYard.Ticket.SECRET, 0) > 0) {
                    throw new IllegalArgumentException("Detective has secret tickets");
                }

                //Check if location of current detective already used by earlier detective
                if (locations.contains(detective.location())) {
                    throw new IllegalArgumentException("Multiple detectives in same location");
                }
                locations.add(detective.location());
            }
        }

        //helper function
        @Nonnull
        private ImmutableSet<Player> getPlayerSet() {
            Set<Player> players = new HashSet<>();
            players.add(this.mrX);
            players.addAll(this.detectives);

            return ImmutableSet.copyOf(players);
        }

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return this.setup;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {

            List<Piece> pieces = this.getPlayerSet()
                    .stream()
                    .map(p -> p.piece())
                    .toList();

            return ImmutableSet.copyOf(pieces);
        }

        @Nonnull
        private Optional<Player> getPlayerFromPiece (Piece piece) {
            Set<Player> players = this.getPlayerSet();
            return players
                    .stream()
                    .filter(p -> p.piece().equals(piece))
                    .findAny();
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            Optional<Player> player = this.detectives
                    .stream()
                    .filter(d -> (d.piece().equals(detective)))
                    .findAny();

            return player.map(p -> p.location());
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            Optional<Player> player = getPlayerSet()
                    .stream()
                    .filter(p -> p.piece().equals(piece))
                    .findAny();

            // Creates TicketBoard implementation from player if player exists.
            return player.map((p) -> new TicketBoard() {
                private final ImmutableMap<ScotlandYard.Ticket, Integer> tickets = p.tickets();

                public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                    return this.tickets.getOrDefault(ticket, 0);
                }
            });

        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return this.log;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return this.winner; // return empty set if no winner
        }

        //Helper function
        private static List<Integer> getListOfDetectiveLocations(List<Player> detectives) {
            return detectives
                    .stream()
                    .map((d) -> d.location())
                    .toList();
        }

        //Helper function
        private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives,
                                                            Player player, int source) {
            Set<Move.SingleMove> moves = new HashSet<>();
            List<Integer> detectiveLocations = getListOfDetectiveLocations(detectives);
            Map<ScotlandYard.Ticket, Integer> tickets = player.tickets();

            for (int destination : setup.graph.adjacentNodes(source)) {
//				If detective not on destination node.
                if (!detectiveLocations.contains(destination)) {

                    for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                        boolean hasSecretTicket = tickets.get(ScotlandYard.Ticket.SECRET) > 0;
                        boolean hasNonSecretTicket = tickets.get(t.requiredTicket()) > 0;
                        if (hasNonSecretTicket || hasSecretTicket) {
                            // Adds move with secret ticket if available
                            if (hasSecretTicket) {
                                moves.add(new Move.SingleMove( 	player.piece(),
                                        source,
                                        ScotlandYard.Ticket.SECRET,
                                        destination )
                                );
                            }

                            // Add only if player has existing non-secret tickets
                            if (hasNonSecretTicket) {
                                moves.add(new Move.SingleMove(	player.piece(),
                                        source,
                                        t.requiredTicket(),
                                        destination  )
                                );
                            }

                        }
                    }
                }
            }

            return moves;
        }


        //Helper function
        private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives,
                                                            Player player, int source){
            Set<Move.DoubleMove> doubleMoves = new HashSet<>();

//			Only runs if player has double tickets.
            if (player.has(ScotlandYard.Ticket.DOUBLE)) {
//				Gets all first moves
                Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, player, source);

//				Checks all possible second moves from each first move.
                for (Move.SingleMove singleMove : singleMoves){

//					Version of player when they used their ticket to move to next
                    Player tempPlayer = player.use(singleMove.ticket);
                    tempPlayer = tempPlayer.at(singleMove.destination);

                    Set<Move.SingleMove> secondMove = makeSingleMoves(setup, detectives, tempPlayer, singleMove.destination);

//					Converts secondMove :: type SingleMove -> type DoubleMove
//					Potentially hoist this into a helper function
                    doubleMoves.addAll(secondMove
                            .stream()
                            .map(
                                    (move2) -> new Move.DoubleMove(
                                            player.piece(),
                                            singleMove.source(),
                                            singleMove.ticket,
                                            move2.source(),
                                            move2.ticket,
                                            move2.destination
                                    )
                            )
                            .toList());
                }

            }
            return doubleMoves;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            Set<Player> players = new HashSet<>();
            Set<Move> moves = new HashSet<>();

//			Only generate moves if no winner.
            if (this.getWinner().isEmpty()) {
//				Converts all remaining pieces into corresponding players.
                players.addAll(this.remaining.stream()
                        .map(p -> {
                            Optional<Player> temp = getPlayerFromPiece(p);
                            if (temp.isEmpty()) throw new IllegalStateException("No remaining players");
                            return temp.get();
                        })
                        .toList());

                for (Player player : players) {
                    moves.addAll(MyGameState.makeSingleMoves(this.setup, this.detectives, player, player.location()));

//					Ensures enough space left in log book for second move.
                    if (this.log.size() < (this.setup.moves.size() - 1) ){
                        moves.addAll(MyGameState.makeDoubleMoves(this.setup, this.detectives, player, player.location()));
                    }
                }
            }
            return ImmutableSet.copyOf(moves);
        }


        //		Helper function to generate the new mrX log based on the current log and new move.
        private ImmutableList<LogEntry> generateNewLog (Move move){
//			Add MrX moves to log.

//			Is list type to ensure log entries are in the correct order.
            List<LogEntry> newLog = new LinkedList<>();
            newLog.addAll(this.log);

            if (move.commencedBy().isMrX()){
                MoveVisitors.SingleMoveVisitor singleMoveVisitor = new MoveVisitors.SingleMoveVisitor();
                List<Move.SingleMove> singleMoves = move.accept(singleMoveVisitor);
                for (Move.SingleMove singleMove : singleMoves) {
                    LogEntry logEntry = null;

                    // True when mrX should reveal his move.
                    if (this.setup.moves.get(newLog.size()) == true) {
                        logEntry = LogEntry.reveal(singleMove.ticket, singleMove.destination);
                    } else {
                        logEntry = LogEntry.hidden(singleMove.ticket);
                    }

                    newLog.add(logEntry);
                }
            }

            return ImmutableList.copyOf(newLog);
        }

        /**
         * Helper function to generateNewRemaining
         * */
        private ImmutableSet<Piece> switchToDetectivesTurn (List<Player> updatedDetectives) {
            ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(
                    this.detectives
                    .stream()
                    .filter(d -> !MyGameState.makeSingleMoves(
                            this.setup,
                            updatedDetectives,
                            d,
                            d.location()
                        ).isEmpty()
                    )
                    .map(d -> d.piece())
                    .toList());

            return newRemaining;
        }
        /**
         * Helper function to generateNewRemaining
         * @param move used
         * @param updatedDetectives list of updated detective states
         * */
        private ImmutableSet<Piece> removedMovedPlayerFromRemaining (Move move, List<Player> updatedDetectives){
            ImmutableSet<Piece> newRemaining = ImmutableSet.copyOf(
                    this.remaining
                    .stream()
                    .filter((piece) -> !piece.equals(move.commencedBy()))
                    .filter(d -> {
                        Optional<Player> optionalPlayer = this.getPlayerFromPiece(d);
                        if (optionalPlayer.isEmpty()) throw new IllegalStateException("Cannot get detective player.");
                        return !MyGameState.makeSingleMoves(
                                this.setup,
                                updatedDetectives,
                                optionalPlayer.get(),
                                optionalPlayer.get().location()).isEmpty();
                    })
                    .toList());

            return newRemaining;
        }

        /**
         * Helper function to advance method
         * Updates the set of remaining players (to move)
         * @param move Move used to advance
         * @param updatedDetectives List of updated detective states
         * @return the updated set of remaining players
         * @throws IllegalStateException if unable to get a detective player
         * */
        private ImmutableSet<Piece> generateNewRemaining (Move move, List<Player> updatedDetectives) {
            ImmutableSet<Piece> newRemaining; // Sets the remaining set to the correct players.

            if (move.commencedBy().isMrX()) {
                newRemaining = switchToDetectivesTurn(updatedDetectives);
            }
            else {
//				Changes to mrX's turn when detective's turns run out and not his turn.
                if (this.remaining.size() <= 1) newRemaining = ImmutableSet.of(mrX.piece());
                else {
                    newRemaining = removedMovedPlayerFromRemaining(move, updatedDetectives);

                    // if there are no remaining players then it's Mr X's turn
                    if (newRemaining.isEmpty()) newRemaining = ImmutableSet.of(Piece.MrX.MRX);
                }
            }
            return newRemaining;
        }

        //		Helper function to advance method to update detective tickets.
        private List<Player> generateNewDetectives (Player player, Set<ScotlandYard.Ticket> singleTickets, int newDestination) {
            List<Player> newDetectives = new LinkedList<>();

            if (player.piece().isDetective()){
//				Update detectives
                Player newPlayer = player.use(singleTickets);
                newPlayer = newPlayer.at(newDestination);
                newDetectives.add(newPlayer);

                Player finalNewPlayer = newPlayer;
                newDetectives.addAll(this.detectives
                        .stream()
                        .filter(d -> !d.piece().equals(finalNewPlayer.piece()))
                        .toList()
                );
            }
            else {
                newDetectives.addAll(this.detectives);
            }

            return newDetectives;
        }

        //		Helper function to advance method to update mrX.
        private Player generateNewMrX (Player player, Set<ScotlandYard.Ticket> singleTickets, int newDestination) {
            Player newMrX;
            if (player.piece().isMrX()) {
                newMrX = player.use(singleTickets);

//				Removes double ticket if used
                if (singleTickets.size() == 2) {
                    newMrX = newMrX.use(ScotlandYard.Ticket.DOUBLE);
                }

                newMrX = newMrX.at(newDestination);
            } else {
                newMrX = this.mrX.give(singleTickets);
            }

            return newMrX;
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!this.moves.contains(move)) {

                throw new IllegalArgumentException("Illegal move: " + move);
            }
            Piece piece = move.commencedBy();
            Optional<Player> playerOptional = getPlayerFromPiece(piece);

            if (playerOptional.isEmpty()) throw new IllegalArgumentException("Move on non existent player");
            Player player = playerOptional.get();

            Set<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

            // Moves player to their new destination.
            int newDestination = move.accept(new MoveVisitors.DestinationVisitor());

            List<Player> updatedDetectives = this.generateNewDetectives(player, tickets, newDestination);

            return new MyGameState(
                    this.setup,
                    this.generateNewRemaining(move, updatedDetectives),
                    this.generateNewLog(move),
                    this.generateNewMrX(player, tickets, newDestination),
                    this.generateNewDetectives(player, tickets, newDestination)
            );
        }
    }

    /**
     * Build a game state for Mr X
     * @param board
     * @return a New Game-state replicating the game that MrX can use directly
     * @throws IllegalArgumentException if the board is already a winning state
     */
    public Board.GameState buildMrXGameState (Board board){
        if (board.getAvailableMoves().isEmpty()) throw new IllegalArgumentException("Board already winning board");

        GameSetup gameSetup = BoardHelpers.generateGameSetup(board);
        Piece mrXPiece = Piece.MrX.MRX;

        // Get MrX
        Player mrX = new Player(
                mrXPiece,
                BoardHelpers.getTicketsForPlayer(board, mrXPiece),
                board.getAvailableMoves().asList().get(0).source()
        );

        return new MyGameState(
                board.getSetup(),
                ImmutableSet.of(mrXPiece),
                board.getMrXTravelLog(),
                mrX,
                BoardHelpers.getDetectives(board)
        );
    }

    /**
     * Build a game state for Detectives
     * @param board Current game state from game
     * @param possibleLocations List of possible locations that MrX could be in
     * @return A list of GameStates for each detective respectively
     */
    public List<Board.GameState> buildDetectiveGameStates (Board board, PossibleLocations possibleLocations) {
        List<Board.GameState> gameStates = new LinkedList<>();
        GameSetup gameSetup = BoardHelpers.generateGameSetup(board);
        List<Piece> remaining = new ArrayList<>();

        //Adds a player who hasn't moved yet
        for (Move move : board.getAvailableMoves()) {
            if (!remaining.contains(move.commencedBy())) {
                remaining.add(move.commencedBy());
            }
        }

        for (int possibleLocation : possibleLocations.getLocations()){
            Player mrX = new Player(
                    Piece.MrX.MRX,
                    BoardHelpers.getTicketsForPlayer(board, Piece.MrX.MRX),
                    possibleLocation
            );

            gameStates.add(
                    new MyGameState(
                            board.getSetup(),
                            ImmutableSet.copyOf(remaining),
                            board.getMrXTravelLog(),
                            mrX,
                            BoardHelpers.getDetectives(board)
                    )
            );
        }
        return gameStates;
    }
}
