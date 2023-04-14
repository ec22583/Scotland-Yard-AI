package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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

    @Test public void testSymmetricRelation(){
        DistancesSingleton distancesSingleton = getDistancesSingleton();

        assertThat(distancesSingleton.get(35,36))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(36,35))
                .isEqualTo(1);

        assertThat(distancesSingleton.get(44,58))
                .isEqualTo(1);
        assertThat(distancesSingleton.get(58,44))
                .isEqualTo(1);
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
