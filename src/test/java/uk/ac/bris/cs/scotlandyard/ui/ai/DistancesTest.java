package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ImmutableValueGraph;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.GREEN;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.RED;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.WHITE;
import static uk.ac.bris.cs.scotlandyard.model.Piece.Detective.YELLOW;
import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

/**
 * Test Distances between the locations are correctly calculated
 * tests written used visual inspection and then verified using distancesSingleton.
 * */
public class DistancesTest extends AITestBase {

    //Distances from a location to itself must be 0
    @Test public void testReflexiveRelalationship(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        for (int i = 1; i < 200; i++){
            assertThat((distancesSingleton.get(i,i)))
                    .isEqualTo(0);
        }
    }

    //Idea: for every node in the graph find its neighbours and test their distances to be equal to 1
    //Since this isn't a directed graph this should be true for both sides making it a symmetric relation
    @Test public void testSymmetricRelation(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        ImmutableValueGraph<Integer, ImmutableSet<Transport>> graph = standardGraph();

        for (int source = 1; source < 200; source++){
            Set<EndpointPair<Integer>> edges = graph.incidentEdges(source);

            for (EndpointPair<Integer> edge : edges){
                int neighbour = edge.nodeU();
                assertThat(distancesSingleton.get(source, neighbour)).isEqualTo(1);
                assertThat(distancesSingleton.get(neighbour, source)).isEqualTo(1);
            }

        }

    }

    @Test public void testTransitiveRelation(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        assertThat(distancesSingleton.get(61,62))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(62,48))
                .isEqualTo(1);

        //Then 61 -> 48 = 2 moves by visual inspection. Therefore, transitive law applies

        assertThat(distancesSingleton.get(61,48))
                .isEqualTo(2);
    }

    //Test tube connections (should be 1)
    @Test public void testTubeConnections(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        //93 -> 79 connection
        assertThat(distancesSingleton.get(93,79))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(93,46))
                .isEqualTo(2);
//        assertThat(distancesSingleton.get(67,111))
//                .isEqualTo(2);
    }

    @Test public void testBusConnections(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        //If used taxis then distance is 87 -> 105 has dist = 3
        //If we used a bus then it is 1
        assertThat(distancesSingleton.get(87,105))
                .isEqualTo(1);

        //Using all other transports would consume 4 distance
        //67 -> 84 -> 85 -> 103 -> 102
        assertThat(distancesSingleton.get(67,102))
                .isEqualTo(1);

    }

    @Test public void testFerryConnections(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();
        assertThat(distancesSingleton.get(194,157))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(157,115))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(115,108))
                .isEqualTo(1);

        assertThat(distancesSingleton.get(194,108))
                .isEqualTo(3);
    }

    //Combination of transport modes
    @Test public void testMultiModalTransport(){

        DistancesSingleton distancesSingleton = getDistancesSingleton();

        //Ferry
        assertThat(distancesSingleton.get(157,194))
                .isEqualTo(1);
        //Taxi
        assertThat(distancesSingleton.get(194,193))
                .isEqualTo(1);
        //Taxi
        assertThat(distancesSingleton.get(193,180))
                .isEqualTo(1);


        assertThat(distancesSingleton.get(157,180))
                .isEqualTo(3);

        //Train
        assertThat(distancesSingleton.get(111,153))
                .isEqualTo(1);
        //Bus
        assertThat(distancesSingleton.get(153,180))
                .isEqualTo(1);

        assertThat(distancesSingleton.get(111,180))
                .isEqualTo(2);
    }
}
