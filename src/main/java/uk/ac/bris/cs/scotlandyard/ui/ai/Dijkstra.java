package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.*;

// Partially inspired by: https://algs4.cs.princeton.edu/44sp/DijkstraUndirectedSP.java.html
public class Dijkstra {
    private int[] distTo;
    private EndpointPair<Integer>[] edgeTo;
    private int startLocation;
    final private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;

    public Dijkstra (
            Board.GameState gameState,
            Piece piece
    ){
        this.graph = gameState.getSetup().graph;
        this.update(gameState, piece);
    }

    public void update(Board.GameState gameState, Piece piece) {
        int numNodes = this.graph.nodes().size();
        this.distTo = new int[numNodes];
        this.edgeTo = new EndpointPair[numNodes];

        if (piece.isMrX()) {
            this.startLocation = gameState.getAvailableMoves().asList().get(0).source();
        } else {
            this.startLocation = gameState.getDetectiveLocation((Piece.Detective) piece).get();
        }

        for (int i = 1; i < numNodes; i++) {
            distTo[i - 1] = Integer.MAX_VALUE;
        }
        distTo[this.startLocation - 1 ] = 0;

        class PairComparator implements Comparator<Pair<Integer, Integer>> {

            @Override
            public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
                return o1.right() - o2.right();
            }
        }

//      Left is node location, right is distance to start.
        Queue<Pair<Integer, Integer>> pq = new PriorityQueue<>(numNodes, new PairComparator());
        pq.add(new Pair(this.startLocation, this.distTo[this.startLocation - 1]));
        while(!pq.isEmpty()) {
//          Extracts the head of the queue.
            Pair<Integer, Integer> current = pq.poll();
            int currentNode = current.left();
            for (EndpointPair<Integer> edge : graph.incidentEdges(currentNode)) {
                int otherNode = edge.adjacentNode(currentNode);
//              All edges are given weight of 1.
                if (distTo[otherNode - 1] > distTo[currentNode - 1] + 1) {
                    distTo[otherNode - 1] = distTo[currentNode - 1] + 1;
                    edgeTo[otherNode - 1] = edge;

                    if (pq.contains(current)) {
                        pq.remove(current);
                        pq.offer(new Pair<>(otherNode, this.distTo[otherNode - 1]));
                    }
                }
            }
        }
    }

    public int getSmallestDistance (int location) {
        return distTo[location - 1];
    }
}
