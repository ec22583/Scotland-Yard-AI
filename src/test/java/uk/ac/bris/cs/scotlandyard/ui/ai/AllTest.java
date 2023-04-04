package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/*
Citation: Closed task cw-model
Adapted for use in the open task.
*/

/**
 * Includes all test for the actual game model. WHen we want to run all tests at once
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        BoardHelpersTest.class,
        PossibleLocationsTest.class
})
public class AllTest {}
