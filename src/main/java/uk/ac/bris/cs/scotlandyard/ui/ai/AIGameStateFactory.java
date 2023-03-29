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
        final private GameSetup setup;
		final private ImmutableSet<Piece> remaining;
		final private ImmutableList<LogEntry> log;
		final private Player mrX;
		final private List<Player> detectives;
		final private ImmutableSet<Move> moves;
		final private ImmutableSet<Piece> winner;

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


		/**
		 *
		 * @param detectives The current detective players for the turn
		 * @param mrX The current mrX player for the turn
		 * @param setup The setup for the game
		 * @param log The current log for the game
		 * @param remaining The current remaining players for the turn
		 * @return A set of the winners for the game; empty if no winners exist
		 */
        static private ImmutableSet<Piece> getWinners(List<Player> detectives,
                                                      Player mrX,
                                                      GameSetup setup,
                                                      List<LogEntry> log,
                                                      ImmutableSet<Piece> remaining){
            boolean detectivesHaveMoves = false;
            for (Player detective : detectives) {
//				If a detective and MrX in same place, detective wins.
                if (detective.location() == mrX.location()) {
//
                   return ImmutableSet.copyOf(detectives
                            .stream()
                            .map(Player::piece)
                            .toList());
                }
//				Checks if any detectives have possible moves remaining.
                if (!MyGameState.makeSingleMoves(setup, detectives, detective, detective.location()).isEmpty()) {
                    detectivesHaveMoves = true;
                }
            }

//			If detectives have no more moves on their turn, MrX wins.
            if (!detectivesHaveMoves) {
				return ImmutableSet.of(mrX.piece());
            }

//			Check for mrX's turn
            if (remaining.contains(mrX.piece())) {
//				If logbook full, mrX wins
                if ((log.size() >= setup.moves.size())) {
					return ImmutableSet.of(mrX.piece());
                }

//				Check MrX can move if it is his turn. If he can't then detectives win
                if (MyGameState.makeSingleMoves(setup, detectives, mrX, mrX.location()).isEmpty()) {
					return ImmutableSet.copyOf(detectives
                            .stream()
                            .map(Player::piece)
                            .toList());
                }
            }
//			If nothing matches, then no one winning yet
			return ImmutableSet.of();
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
					.filter(Objects::nonNull)
					.toList()
					.isEmpty()
			) throw new IllegalArgumentException("Detective is null");
		}

		/**
		 * Helper method. Throws errors if detectives have any illegal states. Includes checking for
		 * illegal tickets and duplicate positions across multiple detectives. Ensures that no game
		 * state will be created with an illegal state.
		 * @param detectives List of current detective states/players
		 */
		private void checkDetectives(final List<Player> detectives) {
			//Stores current locations of detectives
			Set<Integer> locations = new HashSet<>();

			// Inspects all detectives for illegal locations or tickets.
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

		/**
		 * Helper method
		 * @return Set of all players (detectives and mrX)
		 */
		@Nonnull
		private ImmutableSet<Player> getPlayerSet() {
			Set<Player> players = new HashSet<>();
			players.add(this.mrX);
			players.addAll(this.detectives);

			return ImmutableSet.copyOf(players);
		}

		/**
		 * Getter method
		 * @return Current setup for game state.
		 */
		@Nonnull
		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		/**
		 * Getter method
		 * @return Set of all player pieces (not players)
		 */
		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {

			List<Piece> pieces = this.getPlayerSet()
					.stream()
					.map(Player::piece)
					.toList();

			return ImmutableSet.copyOf(pieces);
		}

		/**
		 * Gets corresponding player for the piece passed in.
		 * @param piece Piece to get player of
		 * @return Optional. Has value if player exists and empty if no player exists from piece.
		 */
		@Nonnull
		private Optional<Player> getPlayerFromPiece (Piece piece) {
			Set<Player> players = this.getPlayerSet();
			return players
					.stream()
					.filter(p -> p.piece().equals(piece))
					.findAny();
		}

		/**
		 * Gets the location for a detective if detective exists.
		 * @param detective Piece for a detective
		 * @return Optional. Returns int of location if detective exists and empty if no detective exists.
		 */
		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			Optional<Player> player = this.getPlayerFromPiece(detective);
//			If detective exists, then maps the detective player to a location.
			return player.map(Player::location);
		}

		/**
		 *
		 * @param piece Piece for a player.
		 * @return If player exists, TicketBoard for corresponding player, else empty if no matching player.
		 */
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

		/**
		 * Getter method
		 * @return Current log for game turn
		 */
		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		/**
		 * Getter method
		 * @return Set of winners for current game state.
		 */
		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return this.winner; // return empty set if no winner
		}

		/**
		 * Helper method
		 * @param detectives List of detective players
		 * @return List of location for all detectives
		 */
		private static List<Integer> getListOfDetectiveLocations(List<Player> detectives) {
			return detectives
					.stream()
					.map(Player::location)
					.toList();
		}

		/**
		 * Helper method to get all moves which use a single ticket for player
		 * @param setup Setup for the game
		 * @param detectives List of current detective players
		 * @param player Player to perform moves from
		 * @param source The starting location of the player
		 * @return Set of all single moves from position for the player
		 */
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives,
															Player player, int source) {
			Set<Move.SingleMove> moves = new HashSet<>();
			List<Integer> detectiveLocations = MyGameState.getListOfDetectiveLocations(detectives);
			Map<ScotlandYard.Ticket, Integer> tickets = player.tickets();

			for (int destination : setup.graph.adjacentNodes(source)) {
//				If detective not already occupying destination node.
				if (!detectiveLocations.contains(destination)) {

					for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						boolean hasSecretTicket = tickets.get(ScotlandYard.Ticket.SECRET) > 0;
						boolean hasNonSecretTicket = tickets.get(t.requiredTicket()) > 0;

// 						Adds move if player has secret ticket.
						if (hasSecretTicket) {
							moves.add(new Move.SingleMove( 	player.piece(),
															source,
															ScotlandYard.Ticket.SECRET,
															destination )
							);
						}

//						Add move if player has correct non-secret ticket.
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

			return moves;
		}


		/**
		 * Finds all possible double moves for a player from current position
		 * @param setup The setup for the game
		 * @param detectives List of current detective players
		 * @param player Player to check double moves for
		 * @param source The starting location of the player
		 * @return Set of all double moves possible for player
		 */
		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives,
															Player player, int source){
			Set<Move.DoubleMove> doubleMoves = new HashSet<>();

//			Only runs if player has double tickets.
			if (player.has(ScotlandYard.Ticket.DOUBLE)) {
//				Gets all first moves
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, player, source);

//				Checks all possible second moves from each first move.
				for (Move.SingleMove singleMove : singleMoves){

//					Version of player if they complete first single move
					Player tempPlayer = player.use(singleMove.ticket);
					tempPlayer = tempPlayer.at(singleMove.destination);

//					Checks for all single moves from new temp position.
					Set<Move.SingleMove> secondMove = makeSingleMoves(setup, detectives, tempPlayer, singleMove.destination);

//					Attaches first move to second move to make complete double move.
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

		/**
		 * Gets all possible moves for all remaining players for a turn
		 * @return Set of all moves that can be carried out this turn
		 */
		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> moves = new HashSet<>();

//			Only generate moves if no winner.
			if (this.getWinner().isEmpty()) {
//				Converts all remaining pieces into corresponding players.
				Set<Player> players = new HashSet<>(this.remaining.stream()
						.map(p -> {
							Optional<Player> temp = getPlayerFromPiece(p);
							if (temp.isEmpty())
								throw new IllegalStateException("Cannot match remaining piece to player");

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

		/**
		 * Updates the current log book with a new log book with updated moves
		 * @param move Move to be carried out
		 * @return New log book with new log entry for move (if commenced by mrX)
		 */
		private ImmutableList<LogEntry> generateNewLog (Move move){
			List<LogEntry> newLog = new ArrayList<>(this.getSetup().moves.size());
			newLog.addAll(this.log);

			if (move.commencedBy().isMrX()){
				MoveVisitors.SingleMoveVisitor singleMoveVisitor = new MoveVisitors.SingleMoveVisitor();
				List<Move.SingleMove> singleMoves = move.accept(singleMoveVisitor);

				for (Move.SingleMove singleMove : singleMoves) {
					// True when mrX should reveal his move.
					if (this.setup.moves.get(newLog.size())) {
						newLog.add(LogEntry.reveal(singleMove.ticket, singleMove.destination));
					} else {
						newLog.add(LogEntry.hidden(singleMove.ticket));
					}
				}
			}

			return ImmutableList.copyOf(newLog);
		}

		/**
		 * Updates remaining set to new set of remaining players after move is carried out
		 * @param move The move to be carried out
		 * @return New set of remaining players for turn after move
		 */
        private ImmutableSet<Piece> generateNewRemaining (Move move, List<Player> updatedDetectives) {
            ImmutableSet<Piece> newRemaining; // Sets the remaining set to the correct players.

            if (move.commencedBy().isMrX()) {
                return this.switchToDetectivesTurn(updatedDetectives);
            }
            else {
//				Changes to mrX's turn when detective's turns run out and not his turn.
                if (this.remaining.size() <= 1) {
					return ImmutableSet.of(mrX.piece());
				} else {
                    return this.updateRemainingDetectives(move, updatedDetectives);
                }
            }
        }

		/**
         * Switches from mrX's turn to detective turn.
		 * @return Set of all detective able to move
         */
        private ImmutableSet<Piece> switchToDetectivesTurn (List<Player> updatedDetectives) {
            ImmutableSet<Piece> pieces = ImmutableSet.copyOf(
                    this.detectives
                    .stream()
                    .filter(d -> !MyGameState.makeSingleMoves(
                            this.setup,
                            updatedDetectives,
                            d,
                            d.location()
                        ).isEmpty()
                    )
                    .map(Player::piece)
                    .toList());

			if (pieces.isEmpty()) {
				return ImmutableSet.of(Piece.MrX.MRX);
			}

			return pieces;
        }

		/**
         * Updates remaining detectives to new set of remaining detectives after move and if detectives are unable
		 * to move for any reason
         * @param move Move used this turn
         * @param updatedDetectives List of new detectives after move carried out
		 * @return New remaining set after move carried out
         * */
        private ImmutableSet<Piece> updateRemainingDetectives (Move move, List<Player> updatedDetectives){
            ImmutableSet<Piece> pieces = ImmutableSet.copyOf(
                    this.remaining
                    .stream()
                    .filter((piece) -> !piece.equals(move.commencedBy()))
                    .filter(d -> {
                        Optional<Player> optionalPlayer = this.getPlayerFromPiece(d);
                        if (optionalPlayer.isEmpty()) throw new IllegalStateException("Cannot get detective player.");

//						True if possible next moves exist for detective
                        return !MyGameState.makeSingleMoves(
                                this.setup,
                                updatedDetectives,
                                optionalPlayer.get(),
                                optionalPlayer.get().location()).isEmpty();
                    })
                    .toList());

			if (pieces.isEmpty()) {
				return ImmutableSet.of(Piece.MrX.MRX);
			}
			return pieces;
        }

		/**
		 * Generates new detective players
		 * @param player Player who carried out the turn
		 * @param singleTickets Tickets used for move
		 * @param newDestination New location of player
		 * @return New set of detectives after move carried out
		 */
		private List<Player> generateNewDetectives (Player player, Set<ScotlandYard.Ticket> singleTickets, int newDestination) {
			List<Player> newDetectives = new ArrayList<>(this.detectives.size());

			if (player.piece().isDetective()){
//				If detective moved, update with new state.
				Player newPlayer = player.use(singleTickets);
				newPlayer = newPlayer.at(newDestination);
				newDetectives.add(newPlayer);

				final Player finalNewPlayer = newPlayer;
				newDetectives.addAll(this.detectives
						.stream()
						.filter(d -> !d.piece().equals(finalNewPlayer.piece()))
						.toList()
				);
			} else {
				newDetectives.addAll(this.detectives);
			}

			return newDetectives;
		}

		/**
		 * Generates new mrX from after move is carried out
		 * @param player Player who moved
		 * @param singleTickets Tickets used in move
		 * @param newDestination New location of player
		 * @return New mrX state after move carried out
		 */
		private Player generateNewMrX (Player player, Set<ScotlandYard.Ticket> singleTickets, int newDestination) {
			Player newMrX;
			if (player.piece().isMrX()) {
				newMrX = player.use(singleTickets);

//				Removes double ticket if used
				if (singleTickets.size() == 2) {
					newMrX = newMrX.use(ScotlandYard.Ticket.DOUBLE);
				}

				newMrX = newMrX.at(newDestination);
			}
//			If detective moves, gives MrX used tickets.
			else {
				newMrX = this.mrX.give(singleTickets);
			}

			return newMrX;
		}

		/**
		 * Advances game to the next turn from passed in move
		 * @param move The chosen move to carry out
		 * @return The new game state from after move carried out
		 */
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
					updatedDetectives
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
        List<Board.GameState> gameStates = new ArrayList<>(possibleLocations.getLocations().size());
		final ImmutableList<Player> detectives = BoardHelpers.getDetectives(board);
		List<Piece> remaining = new ArrayList<>(detectives.size());

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
                            detectives
                    )
            );
        }
        return gameStates;
    }
}
