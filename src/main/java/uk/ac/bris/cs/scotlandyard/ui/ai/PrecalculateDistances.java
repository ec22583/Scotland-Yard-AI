package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.*;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Class that creates a file which has all pre-calculated distances from nodes to other nodes (ignore ticket type)
 * using Dijkstra's algorithm. The file will be used as a lookup table of O(1) time for Localization Categorization
 * and E-Greedy playouts heuristic.
 * */
@SuppressWarnings("UnstableApiUsage")
public class PrecalculateDistances {
    private Table<Integer, Integer, Integer> distances;
    private ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;

    public PrecalculateDistances () {
        try {

            //Create the text file for the graph
            graph = ScotlandYard.readGraph(
                    Resources.toString(
                            Resources.getResource("graph.txt"),
                            StandardCharsets.UTF_8)
            );
            int min = graph.nodes().stream().mapToInt(Integer::intValue).min().orElseThrow();
            int max = graph.nodes().stream().mapToInt(Integer::intValue).max().orElseThrow();

//          Create a list of nodes of the graph (1-199)
            List<Integer> range = IntStream.rangeClosed(min, max).boxed().toList();

//          199 * 199 table to store distances
            distances = ArrayTable.create(range, range);
        } catch (IOException e) {
            System.err.println("Could not read graph from 'graph.txt'");
            System.exit(1);
        }
    }

    public void run () throws IOException {
        //Iterate through each row (represents a node and distance to all other nodes)
        for (int startPosition : distances.rowKeySet()) {
            Map<Integer, Integer> row = distances.row(startPosition);
            //Sets up distance between a node and all other nodes as functionally infinity
            row.keySet().forEach((node) -> row.replace(node, Integer.MAX_VALUE));


            dijkstras(startPosition, row);
        }

        FileWriter fileWriter = new FileWriter("./distances.txt");

        distances.rowMap().forEach((rowKey, columnSet) -> {
            columnSet.forEach((columnKey, distance) -> {
                try {
                    fileWriter.append(String.format("%s,%s,%s\n", rowKey, columnKey, distance));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        System.out.println("Finished writing distances to 'distances.txt'");

        fileWriter.close();
    }

    public void dijkstras (int startPosition, Map<Integer, Integer> row) {
//      Sorts into order based on current distance from start location (row::get gets distance for a location).
        PriorityQueue<Integer> queue = new PriorityQueue<>(Comparator.comparing(row::get));

//      Distance to self is 0 (graph is known to not have self-loops)
        row.put(startPosition, 0);
        queue.add(startPosition);

        int currentPosition;

        while(!queue.isEmpty()) {

//          Retrieve the node with the shortest overall distance (and remove it from queue)
            currentPosition = queue.poll();
            int currentDistance = row.get(currentPosition);
            Set<EndpointPair<Integer>> edges = graph.incidentEdges(currentPosition);

            int finalCurrentPosition = currentPosition;

//          Extract all connections regardless of ticket type
            List<Integer> connectingNodes = edges
                    .stream()
                    .map((edge) -> edge.adjacentNode(finalCurrentPosition))
                    .toList();


            for (int connectingNode : connectingNodes) {

//              If connecting node's distance is more than current shortest distance + 1
                if (row.get(connectingNode) > currentDistance + 1) {
                    row.put(connectingNode, currentDistance + 1);

//                  Remove the connecting node from queue once processed
                    queue.remove(connectingNode);
                    queue.add(connectingNode);
                }
            }
        }
    }

    public static void main (String[] args) {
        PrecalculateDistances precalculateDistances = new PrecalculateDistances();
        try {
         precalculateDistances.run();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
