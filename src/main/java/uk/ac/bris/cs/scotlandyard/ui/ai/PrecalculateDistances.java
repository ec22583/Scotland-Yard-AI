package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.*;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class PrecalculateDistances {
    private Table<Integer, Integer, Integer> distances;
    private int min;
    private int max;
    private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;

    public PrecalculateDistances () {
        try {
            graph = ScotlandYard.readGraph(
                    Resources.toString(
                            Resources.getResource("graph.txt"),
                            StandardCharsets.UTF_8)
            );
            min = graph.nodes().stream().mapToInt(Integer::intValue).min().orElseThrow();
            max = graph.nodes().stream().mapToInt(Integer::intValue).max().orElseThrow();
            List<Integer> range = IntStream.rangeClosed(min, max).boxed().toList();

            distances = ArrayTable.create(range, range);
        } catch (IOException e) {
            System.err.println("Could not read graph from 'graph.txt'");
            System.exit(1);
        }
    }


    public void run () {
        for (int startPosition : distances.rowKeySet()) {
            Map<Integer, Integer> row = distances.row(startPosition);
            row.keySet().forEach(k -> row.replace(k, Integer.MAX_VALUE));
            dijkstras(startPosition, row);
        }

        System.out.println(distances.values());
        System.out.println("Finished running epic dijkstra's");
    }

    public void dijkstras (int startPosition, Map<Integer, Integer> row) {
//      Sorts into order based on current distance from start location.
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparing(row::get));
        row.put(startPosition, 0);
        queue.add(startPosition);

        int currentPosition;
        while(!queue.isEmpty()) {
            currentPosition = queue.poll();
            int currentDistance = row.get(currentPosition);
            Set<EndpointPair<Integer>> edges = graph.incidentEdges(currentPosition);

            int finalCurrentPosition = currentPosition;
            List<Integer> connectingNodes = edges
                    .stream()
                    .map(e -> e.adjacentNode(finalCurrentPosition))
                    .toList();

            for (int connectingNode : connectingNodes) {
                if (row.get(connectingNode) > currentDistance + 1) {
                    row.put(connectingNode, currentDistance + 1);
                    if (queue.contains(connectingNode)) {
                        queue.remove(connectingNode);
                    }
                    queue.add(connectingNode);
                }
            }
        }
    }

    public static void main () {
        PrecalculateDistances precalculateDistances = new PrecalculateDistances();
        precalculateDistances.run();
    }
}
