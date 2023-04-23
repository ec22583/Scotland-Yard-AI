package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.util.*;

// A factory to create game states such that the AIs can use it
// We took this from our Cw-Model coursework (closed task) and modified it for the open task
public class AIGameStateFactory {
    @SuppressWarnings("UnstableApiUsage")
	private final static class MyGameState implements AIGameState {
        final private GameSetup setup;
		final private ImmutableSet<Piece> remaining;
		final private ImmutableList<LogEntry> log;
		final private Player mrX;
		final private List<Player> detectives;
		final private ImmutableSet<Move> moves;
		final private ImmutableSet<Piece> winner;
		final private Move previousMove;

		private MyGameState(final GameSetup setup,
							final ImmutableSet<Piece> remaining,
							final ImmutableList<LogEntry> log,
							final Player mrX,
							final List<Player> detectives,
							final Move previousMove) {

			checkParamsNotNull(setup, remaining, mrX, detectives);
			checkDetectives(detectives);

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.previousMove = previousMove;

			this.winner = MyGameState.getWinners(
					this.detectives,
					this.mrX,
					this.setup,
					this.log,
					this.remaining);
			this.moves = MyGameState.generateAvailableMoves(
					this.detectives,
					this.mrX,
					this.remaining,
					this.winner,
					this.log,
					this.setup
			);

		}


		/**
		 * Generates the current winners for the game state.
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
			List<Integer> detectiveLocations = MyGameState.getListOfDetectiveLocations(detectives);
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
                if (!detectivesHaveMoves) {
					if (!MyGameState.makeSingleMoves(
							setup,
							detectiveLocations,
							detective,
							detective.location()
					).isEmpty()) {
						detectivesHaveMoves = true;
					}
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
                if (MyGameState.makeSingleMoves(setup, detectiveLocations, mrX, mrX.location()).isEmpty()) {
					return ImmutableSet.copyOf(detectives
                            .stream()
                            .map(Player::piece)
                            .toList());
                }
            }
//			If nothing matches, then no one winning yet
			return ImmutableSet.of();
        }

		/**
		 * Ensures that all the required parameters are not null
		 *
		 * @param setup Game setup
		 * @param remaining The current remaining pieces for turn
		 * @param mrX Mr X player
		 * @param detectives List of detective players
		 * @throws IllegalArgumentException if any required parameters are null
		 */
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
		 * Helper method. Throws errors if detectives have any illegal states.
		 * Includes checking for illegal tickets and duplicate positions across
		 * multiple detectives. Ensures that no game state will be created with
		 * an illegal state.
		 *
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
		 *
		 * @return Set of all players in game (detectives and mrX)
		 */
		@Nonnull
		private ImmutableSet<Player> getPlayerSet() {
			ImmutableSet.Builder<Player> builder = ImmutableSet.builder();
			builder.addAll(this.detectives);
			builder.add(this.mrX);
			return builder.build();
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
					.map(Player::piece)
					.toList();

			return ImmutableSet.copyOf(pieces);
		}

		/**
		 * Gets corresponding player for the piece passed in.
		 *
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

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			Optional<Player> player = this.getPlayerFromPiece(detective);
//			If detective exists, then maps the detective player to a location.
			return player.map(Player::location);
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

		/**
		 * Helper method
		 *
		 * @param detectives List of detective players
		 * @return List of location for all detectives in detectives list
		 */
		private static List<Integer> getListOfDetectiveLocations(List<Player> detectives) {
			return detectives
					.stream()
					.map(Player::location)
					.toList();
		}

		/**
		 * Extra added method to help average distance calculations.
		 *
		 * @return List of all the locations for the detectives.
		 */
		public List<Integer> getDetectiveLocations () {
			return getListOfDetectiveLocations(detectives);
		}

		public int getMrXLocation () {
			return this.mrX.location();
		}

		/**
		 * Helper method to get all moves which use a single ticket for player and add
		 * them to passed in builder.
		 *
		 * @param setup              Setup for the game
		 * @param detectiveLocations List of current detective players
		 * @param player             Player to perform moves from
		 * @param source             The starting location of the player
		 * @return Set of all single moves from position for the player
		 */
		private static Set<Move.SingleMove> makeSingleMoves(
				GameSetup setup,
				List<Integer> detectiveLocations,
				Player player,
				int source ) {
			Set<Move.SingleMove> moves = new HashSet<>();
			Map<ScotlandYard.Ticket, Integer> tickets = player.tickets();
			boolean hasSecretTicket = tickets.get(ScotlandYard.Ticket.SECRET) > 0;

			for (int destination : setup.graph.adjacentNodes(source)) {
//				If detective not already occupying destination node.
				if (!detectiveLocations.contains(destination)) {

// 					Adds move if player has secret ticket.
					if (hasSecretTicket) {
						moves.add(new Move.SingleMove( 	player.piece(),
														source,
														ScotlandYard.Ticket.SECRET,
														destination )
						);
					}

					for (ScotlandYard.Transport t : Objects.requireNonNull(
							setup.graph.edgeValueOrDefault(
									source,
									destination,
									ImmutableSet.of()
							)
					)) {
						boolean hasNonSecretTicket = tickets.get(t.requiredTicket()) > 0;

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
		 *
		 * @param setup              The setup for the game
		 * @param detectiveLocations List of current detective players
		 * @param player             Player to check double moves for
		 * @param source             The starting location of the player
		 * @return Set of all double moves possible for player
		 */
		private static Set<Move.DoubleMove> makeDoubleMoves(
				GameSetup setup,
				List<Integer> detectiveLocations,
				Player player,
				int source ){
			Set<Move.DoubleMove> doubleMoves = new HashSet<>();

//			Only runs if player has double tickets.
			if (player.has(ScotlandYard.Ticket.DOUBLE)) {
//				Gets all first moves
				Set<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectiveLocations, player, source);

//				Checks all possible second moves from each first move.
				for (Move.SingleMove singleMove : singleMoves){

//					Version of player if they complete first single move
					Player tempPlayer = player.use(singleMove.ticket);
					tempPlayer = tempPlayer.at(singleMove.destination);

//					Checks for all single moves from new temp position.
					Set<Move.SingleMove> secondMove = makeSingleMoves(setup, detectiveLocations, tempPlayer, singleMove.destination);

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

		@Nonnull
		@Override
		public Optional<Move> getPreviousMove() {
			return Optional.ofNullable(this.previousMove);
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			return this.moves;
		}

		/**
		 * Generates all the moves that can be played by players during the turn.
		 *
		 * @param detectives List of all detective players
		 * @param mrX Mr X player
		 * @param remaining Set of all remaining pieces for turn
		 * @param winners Set of all winning pieces for turn
		 * @param log Mr X log for game
		 * @param setup Game setup for game
		 * @return ImmutableSet of all possible moves which can be made.
		 */
		private static ImmutableSet<Move> generateAvailableMoves(List<Player> detectives,
																  Player mrX,
																  Set<Piece> remaining,
																  Set<Piece> winners,
																  List<LogEntry> log,
																  GameSetup setup) {
			ImmutableSet.Builder<Move> builder = ImmutableSet.builder();
			List<Integer> detectiveLocations = MyGameState.getListOfDetectiveLocations(detectives);

//			Only generate moves if no winner.
			if (winners.isEmpty()) {
				List<Player> players = new ArrayList<>(detectives.size() + 1);

//				Converts all remaining pieces into corresponding players.
				if (remaining.contains(mrX.piece())) {
					players.add(mrX);
				} else {
					for (Player detective : detectives) {
						if (remaining.contains(detective.piece())) {
							players.add(detective);
						}
					}
				}

				for (Player player : players) {
					builder.addAll(MyGameState.makeSingleMoves(setup, detectiveLocations, player, player.location()));

//					Ensures enough space left in log book for second move.
					if (log.size() < (setup.moves.size() - 1) ){
						builder.addAll(MyGameState.makeDoubleMoves(setup, detectiveLocations, player, player.location()));
					}
				}
			}
			return builder.build();
		}

		/**
		 * Updates the current log book to a new log book with current move appended.
		 *
		 * @param move Move to be carried out
		 * @return New log book with new log entry for move (if commenced by mrX)
		 */
		private ImmutableList<LogEntry> generateNewLog (Move move){
			ImmutableList.Builder<LogEntry> builder =
					ImmutableList.builderWithExpectedSize(this.getMrXTravelLog().size() + 2);
			builder.addAll(this.log);

			if (move.commencedBy().isMrX()){
				MoveVisitors.SingleMoveVisitor singleMoveVisitor = new MoveVisitors.SingleMoveVisitor();
				List<Move.SingleMove> singleMoves = move.accept(singleMoveVisitor);
				for (int i = 0; i < singleMoves.size(); i++) {
					// True when mrX should reveal his move.
					if (this.setup.moves.get(this.getMrXTravelLog().size() + i)) {
						builder.add(LogEntry.reveal(singleMoves.get(i).ticket, singleMoves.get(i).destination));
					} else {
						builder.add(LogEntry.hidden(singleMoves.get(i).ticket));
					}
				}
			}

			return builder.build();
		}

		/**
		 * Updates remaining set to new set of remaining players after move is carried out.
		 *
		 * @param move The move to be carried out
		 * @return New set of remaining players for turn after move
		 */
        private ImmutableSet<Piece> generateNewRemaining (Move move, List<Player> updatedDetectives) {
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
		 *
		 * @return Set of all detective able to move
         */
        private ImmutableSet<Piece> switchToDetectivesTurn (List<Player> updatedDetectives) {
			final List<Integer> detectiveLocations = updatedDetectives
					.stream()
					.map(Player::location)
					.toList();

            ImmutableSet<Piece> pieces = ImmutableSet.copyOf(
                    this.detectives
                    .stream()
                    .filter(d -> !MyGameState.makeSingleMoves(
							this.setup,
							detectiveLocations,
							d,
							d.location())
							.isEmpty()
					)
                    .map(Player::piece)
                    .toList());

			if (pieces.isEmpty()) {
				return ImmutableSet.of(Piece.MrX.MRX);
			}

			return pieces;
        }

		/**
         * Updates remaining detectives to new set of remaining detectives after move and
		 * filters out detectives unable to move for any reason
		 *
         * @param move Move used this turn
         * @param updatedDetectives List of new detectives after move carried out
		 * @return New remaining set after move carried out
         * */
        private ImmutableSet<Piece> updateRemainingDetectives (Move move, List<Player> updatedDetectives){
			final List<Integer> detectiveLocations = updatedDetectives.stream()
					.map(Player::location)
					.toList();

            ImmutableSet<Piece> pieces = ImmutableSet.copyOf(
                    this.getPlayerSet()
                    .stream()
//					Filters so only remaining players remain.
					.filter(p -> this.remaining.contains(p.piece()))

//					Filters so players who didn't move remain.
                    .filter((p) -> !(p.piece()).equals(move.commencedBy()))

//					Filters so only players who have available moves this turn remain.
                    .filter(d -> {
						// Check if location after making a single move is empty, then filter it out if it is
                        return !MyGameState.makeSingleMoves(
                                this.setup,
                                detectiveLocations,
                                d,
                                d.location()).isEmpty();
                    })
					.map(Player::piece)
                    .toList());

			if (pieces.isEmpty()) return ImmutableSet.of(Piece.MrX.MRX);

			return pieces;
        }

		/**
		 * Generates new detective players once Mr X has moved.
		 * Filters out detectives unable to move for any reason.
		 *
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
			}
			else {
				newDetectives.addAll(this.detectives);
			}

			return newDetectives;
		}

		/**
		 * Generates new mrX after move is carried out.
		 *
		 * @param player Player who moved
		 * @param singleTickets Tickets used in move
		 * @param newDestination New location of player
		 * @return New mrX state after move carried out
		 */
		private Player generateNewMrX (Player player, List<ScotlandYard.Ticket> singleTickets, int newDestination) {
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

		@Nonnull
		@Override
		public AIGameState advance(Move move) {
			if (!this.moves.contains(move)) {
				throw new IllegalArgumentException("Illegal move: " + move);
			}

			Piece piece = move.commencedBy();
			Player player = getPlayerFromPiece(piece).orElseThrow();

			List<ScotlandYard.Ticket> tickets = move.accept(new MoveVisitors.TicketVisitor());

			// Moves player to their new destination.
			int newDestination = move.accept(new MoveVisitors.DestinationVisitor());

			List<Player> updatedDetectives = this.generateNewDetectives(player, ImmutableSet.copyOf(tickets), newDestination);

			return new MyGameState(
					this.setup,
					this.generateNewRemaining(move, updatedDetectives),
					this.generateNewLog(move),
					this.generateNewMrX(player, tickets, newDestination),
					updatedDetectives,
					move
			);
		}

		public List<Integer> getGameStateList () {
			List<Integer> output = new ArrayList<>();
			output.add(mrX.location());
			output.addAll(detectives.stream().map(Player::location).toList());
			List<ImmutableMap<ScotlandYard.Ticket, Integer>> detectivesTickets = detectives.stream().map(Player::tickets).toList();
			for (ImmutableMap<ScotlandYard.Ticket, Integer> detectiveTickets : detectivesTickets) {
				output.add(detectiveTickets.get(ScotlandYard.Ticket.TAXI));
				output.add(detectiveTickets.get(ScotlandYard.Ticket.BUS));
				output.add(detectiveTickets.get(ScotlandYard.Ticket.UNDERGROUND));
			}
			ImmutableMap<ScotlandYard.Ticket, Integer> mrXTickets = mrX.tickets();
			output.add(mrXTickets.get(ScotlandYard.Ticket.TAXI));
			output.add(mrXTickets.get(ScotlandYard.Ticket.BUS));
			output.add(mrXTickets.get(ScotlandYard.Ticket.UNDERGROUND));
			output.add(mrXTickets.get(ScotlandYard.Ticket.DOUBLE));
			output.add(mrXTickets.get(ScotlandYard.Ticket.SECRET));
			output.add(getSetup().moves.size() - getMrXTravelLog().size());

			return output;
		}
    }

    /**
     * Build a game state for Mr X AI.
	 *
     * @param board The current board for the game state
     * @return a New Game-state replicating the game that MrX can use directly
     * @throws IllegalArgumentException if the board is already a winning state
     */
    public AIGameState buildMrXGameState (Board board){
        if (board.getAvailableMoves().isEmpty()) throw new IllegalArgumentException("Board already winning board");
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
                BoardHelpers.getDetectives(board),
				null
        );
    }

    /**
     * Build all possible game states for detective AI.
	 *
     * @param board Current game state from game
     * @param possibleLocations List of possible locations that MrX could be in
     * @return A list of GameStates for each detective respectively
     */
    public List<Pair<AIGameState, Integer>> buildDetectiveGameStates (Board board, PossibleLocations possibleLocations) {
        List<Pair<AIGameState, Integer>> gameStates = new ArrayList<>(possibleLocations.getLocations().size());
		final ImmutableList<Player> detectives = BoardHelpers.getDetectives(board);
		List<Piece> remaining = new ArrayList<>(detectives.size());

        //Adds a player who hasn't moved yet
        for (Move move : board.getAvailableMoves()) {
            if (!remaining.contains(move.commencedBy())) {
                remaining.add(move.commencedBy());
            }
        }

        for (int possibleLocation : possibleLocations.getLocations()){

			//Construct a new MrX per each possible location
            Player mrX = new Player(
                    Piece.MrX.MRX,
                    BoardHelpers.getTicketsForPlayer(board, Piece.MrX.MRX),
                    possibleLocation
            );

			AIGameState aiGameState = new MyGameState(
					board.getSetup(),
					ImmutableSet.copyOf(remaining),
					board.getMrXTravelLog(),
					mrX,
					detectives,
					null
			);

            gameStates.add(new Pair<>(aiGameState, possibleLocation));
        }
        return gameStates;
    }

		/**
	 * Builds the initial game state for the game
	 * @param setup the game setup
	 * @param mrX MrX player
	 * @param detectives detective players
	 * @return Initial game state for game
	 */
	@Nonnull
	public AIGameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {

		return new MyGameState(
				setup,
				ImmutableSet.of(Piece.MrX.MRX),
				ImmutableList.of(),
				mrX,
				detectives,
				null);
	}
}
