//package uk.ac.bris.cs.scotlandyard.ui.ai;
//
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableMap;
//import com.google.common.collect.ImmutableSet;
//import com.google.common.graph.ImmutableValueGraph;
//import uk.ac.bris.cs.scotlandyard.model.*;
//
//import javax.annotation.Nonnull;
//import java.awt.*;
//import java.lang.reflect.Array;
//import java.util.*;
//import java.util.List;
//
//public class AIGameStateFactory {
//    private class AIGameState implements Board.GameState {
//        private Board.GameState gameState;
//        private final List<Piece> pieces; //Must be ordered properly
//        private int currentPiece; //index of the piece in pieces
//        private final List<Move> moves;
//
//        public AIGameState (GameSetup gameSetup, Piece currentPiece, Player mrX, ImmutableList<Player> detectives) {
//            List<Player> currentPlayers = new ArrayList<>(detectives.size() + 1);
//            currentPlayers.add(mrX);
//            currentPlayers.addAll(detectives);
//
////          Sorts the list based on hex code of the pieces, guaranteeing that MrX will be first
////          Post-condition: MrX is first
//            currentPlayers = currentPlayers
//                    .stream()
//                    .sorted(Comparator
//                            .comparingInt(o -> Color.decode(o.piece().webColour()).getRGB()))
//                    .toList();
//
//            this.pieces = new ArrayList<>(currentPlayers.size());
//            this.pieces
//                    .addAll(currentPlayers
//                            .stream()
//                            .map(p -> p.piece())
//                            .toList());
//            this.currentPiece = this.pieces.indexOf(currentPiece);
//
//            //Selecting a possible move to get to the current location
//            List<Move> tempMoves = new LinkedList<>();
//            List<Player> tempPlayers = new LinkedList<>();
//
//            ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> gameGraph =
//                    gameSetup.graph;
//
//            //In a gamestate MrX must be first but if it's a detective's turn then
//            //This sloopshould make it such that MrX should still be first
//            for (int i = 0; i < this.currentPiece; i++) {
//
//                // Get current player information
//                Player currentPlayer = currentPlayers.get(i);
//                int currentLocation = currentPlayer.location();
//
//                //get possible destinations
//                List<Integer> possibleDestinations = gameGraph
//                        .asGraph()
//                        .incidentEdges(currentLocation)
//                        .stream()
//                        .parallel()
//                        .map(e -> e.adjacentNode(currentLocation))
//                        .toList();
//
//                // Just in case
//                if (possibleDestinations.isEmpty())
//                    throw new IllegalStateException("Graph node has no adjacent nodes");
//
//                int destination = possibleDestinations.get(0); //Get the first destination
//                Optional<ImmutableSet<ScotlandYard.Transport>> optionalTickets =
//                        gameGraph.edgeValue(currentLocation, destination);
//
//                if (optionalTickets.isEmpty()) throw new IllegalStateException("Cannot find tickets for edge");
////                if (optionalTickets.get().isEmpty()) throw new IllegalStateException("No tickets available for edge");
//
//                //get the required ticket for the first type of transport available
//                ScotlandYard.Ticket ticket = optionalTickets.get().asList().get(0).requiredTicket();
//
//                //Make a temporary player, gives them a ticket, moves them to the location
//                //Adds them to the tempPlayers list
//                Player tempPlayer = currentPlayer;
//                tempPlayer = tempPlayer.give(ticket);
//                tempPlayer = tempPlayer.at(destination);
//                tempPlayers.add(tempPlayer);
//
//                Move move = new Move.SingleMove(currentPlayer.piece(), destination, ticket, currentLocation);
//                tempMoves.add(move);
//            }
//
//            Player tempMrX;
//            if (!tempPlayers.isEmpty()) tempMrX = tempPlayers.get(0);
//            else tempMrX = mrX;
//
//            List<Player> tempDetectives = new LinkedList<>();
//            // If there are other players in the list of temporary players
//            if (tempPlayers.size() > 1) {
//                tempPlayers.remove(0);
//                tempDetectives.addAll(tempPlayers);
//            }
//            else {
//                tempDetectives = detectives;
//            }
//
//            MyGameStateFactory myGameStateFactory = new MyGameStateFactory();
//            GameState tempGameState = myGameStateFactory.build(
//                    gameSetup,
//                    tempMrX,
//                    ImmutableList.copyOf(tempDetectives)
//            );
//
//            for (Move tempMove : tempMoves) {
//                tempGameState = tempGameState.advance(tempMove);
//            }
//
//            this.gameState = tempGameState;
//
//            this.moves = new LinkedList<>();
//        }
//
//        public GameSetup getGameSetup () {
//            return this.gameState.getSetup();
//        }
//
//        public Piece getCurrentPiece () {
//                return this.pieces.get(this.currentPiece);
//            }
//
//
//        @Nonnull @Override
//        public GameState advance(Move move) {
//
//            AIGameState newAIGameState = new AIGameState(
//              this.getGameSetup(),
//              (this.currentPiece + 1) % this.pieces.size(),
//
//            );
//
//
//            this.moves.add(move);
////          Moves current piece to next piece to play a turn.
//            this.currentPiece = (this.currentPiece + 1) % this.pieces.size();
//            this.gameState = this.gameState.advance(move);
//
//            return this;
//        }
//
//        @Nonnull @Override
//        public GameSetup getSetup() {
//            return this.gameState.getSetup();
//        }
//
//        @Nonnull @Override
//        public ImmutableSet<Piece> getPlayers() {
//            return this.gameState.getPlayers();
//        }
//
//        @Nonnull @Override
//        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
//            return this.gameState.getDetectiveLocation(detective);
//        }
//
//        @Nonnull @Override
//        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
//            return this.gameState.getPlayerTickets(piece);
//        }
//
//        @Nonnull @Override
//        public ImmutableList<LogEntry> getMrXTravelLog() {
//            return this.gameState.getMrXTravelLog();
//        }
//
//        @Nonnull @Override
//        public ImmutableSet<Piece> getWinner() {
//            return this.gameState.getWinner();
//        }
//
//        @Nonnull @Override
//        public ImmutableSet<Move> getAvailableMoves() {
//            return ImmutableSet.copyOf(this.gameState
//                    .getAvailableMoves()
//                    .stream()
//                    .parallel()
//                    .filter(m -> m.commencedBy().equals(this.currentPiece))
//                    .toList());
//        }
//    }
//
//    //Helper method
//    static private ImmutableMap<ScotlandYard.Ticket, Integer> getTicketsForPlayer (Board board, Piece piece) {
//
////      Get a TicketBoard of tickets
//        Board.TicketBoard tickets =
//                board.getPlayerTickets(board
//                        .getPlayers()
//                        .stream()
//                        .parallel()
//                        .filter(p -> p.equals(piece))
//                        .findAny()
//                        .get()
//                ).get();
//
//        //      Generates map of ticket values from current TicketBoard state.
//        Map<ScotlandYard.Ticket, Integer> ticketMap = new HashMap<>();
//        for (ScotlandYard.Ticket ticketType : ScotlandYard.Ticket.values()) {
//            ticketMap.put(ticketType, tickets.getCount(ticketType));
//        }
//
//        return ImmutableMap.copyOf(ticketMap);
//    }
//
//    //Helper function. Get detectives
//    private ImmutableList<Player> getDetectives (Board board){
//
//        List<Player> detectives = new LinkedList<>(board
//                .getPlayers()
//                .stream()
//                .parallel()
//                .filter(p -> p.isDetective())
//                .map(piece -> new Player(
//                                piece,
//                                // Generates tickets for piece.
//                                getTicketsForPlayer(board, piece),
//                                //  Piece must be cast to a Detective. Not an issue since mrx filtered out earlier
//                                // (For type safety). .get() fine as piece always is a detective.
//                                board.getDetectiveLocation((Piece.Detective) piece).get()
//                        )
//                )
//                .toList()
//        );
//
//        return ImmutableList.copyOf(detectives);
//    }
//
//    public ImmutableList<Board.GameState> generateGameStates (Board board) {
//        // if it's MrX's current turn
//        if (board.getAvailableMoves().asList().get(0).commencedBy().equals(Piece.MrX.MRX)) {
//            //Make a MrX
//            Player mrX = new Player(
//                    Piece.MrX.MRX,
//                    getTicketsForPlayer(board, Piece.MrX.MRX),
//                    board.getAvailableMoves().asList().get(0).source()
//            );
//
//            List<Player> detectives = this.getDetectives(board);
//            List<Board.GameState> gameStates = new LinkedList<>();
//            gameStates.add(build(board, mrX, ImmutableList.copyOf(detectives)));
//            return ImmutableList.copyOf(gameStates);
//        }
//        else {
//
//        }
//        return null;
//    }
//
//    private ImmutableList<Board.GameState> findNextPossibleGameStates (List<Board.GameState> oldGameStates) {
//       return null;
//    }
//
////  Pre-requisite: Board is initial start board.
//    private ImmutableList<Board.GameState> buildStartGameStates (Board board) {
//        int[] startLocations = {35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172}; //Preset start locations
//        List<Integer> possibleLocations = new LinkedList<>();
//        Arrays.stream(startLocations)
//                .forEach(l -> possibleLocations.add(l));
//
//        List<Integer> detectiveLocations = new LinkedList<>();
//
//        for (Piece player : board.getPlayers()) {
//            if (player.isDetective()) {
//                Optional<Integer> locationOptional = board.getDetectiveLocation((Piece.Detective) player);
//                if (locationOptional.isEmpty()) throw new IllegalStateException("Detective has no valid location");
//
//                possibleLocations.remove(locationOptional.get());
//                detectiveLocations.add(locationOptional.get());
//            }
//        }
//
//        List<Player> detectives = getDetectives(board);
//
//        List<Board.GameState> gameStates = new LinkedList<>();
//        if (board.getAvailableMoves().isEmpty()) throw new IllegalArgumentException("Board already winning state");
//        Piece currentPlayer = board.getAvailableMoves().asList().get(0).commencedBy();
//
//        for (int possibleLocation : possibleLocations){
//            Player mrX = new Player(Piece.MrX.MRX, getTicketsForPlayer(board, Piece.MrX.MRX), possibleLocation);
//            gameStates.add(build(board, mrX,  ImmutableList.copyOf(detectives)));
//        }
//
//
//        return ImmutableList.copyOf(gameStates);
//    }
//
//    public Board.GameState build (Board board, Player mrX, ImmutableList<Player> detectives) {
//        List<Boolean> moveSetup = new LinkedList<>();
//        if (board.getAvailableMoves().isEmpty()) throw new IllegalArgumentException("Already in win state");
//        Piece currentPiece = board.getAvailableMoves().asList().get(0).commencedBy();
//
////      Ensures that MrX is always visible to AI.
//        for (int i = 0; i < (board.getSetup().moves.size() - board.getMrXTravelLog().size()); i++) {
//            moveSetup.add(true);
//        }
//
//        return (new AIGameState(
//            new GameSetup(board.getSetup().graph, ImmutableList.copyOf(moveSetup)),
//                currentPiece,
//                mrX,
//                detectives
//        ));
//    }
//}
//
//
