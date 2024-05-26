package com.example.APITestingPlugin.enums;

import com.example.EthanApiPlugin.Collections.NPCs;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Enum representing various API tests.
 */
public enum APITests {
    NPC_COLLECTIONS("NPC COLLECTIONS", Arrays.asList(
            new Assertion("NPCS at GE", Arrays.asList(
                    () -> NPCs.search().result().size()  // Return the actual size for the final check
            ), 14), // The expected result after the last function
            new Assertion("Bankers at GE (withName)", Arrays.asList(
                    () -> NPCs.search().withName("Banker").result().size()  // Return the actual size for the final check
            ), 4) // The expected result after the last function
    ));

    private final String testName;
    private final List<Assertion> assertions;

    /**
     * Constructor for the APITests enum.
     *
     * @param testName   the name of the test category
     * @param assertions a list of Assertion representing the test logic
     */
    APITests(String testName, List<Assertion> assertions) {
        this.testName = testName;
        this.assertions = assertions;
    }

    /**
     * Gets the name of the test category.
     *
     * @return the name of the test category
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Gets the list of assertions.
     *
     * @return the list of assertions
     */
    public List<Assertion> getAssertions() {
        return assertions;
    }

    /**
     * Class representing an assertion in the API tests.
     */
    public static class Assertion {
        private final String description;
        private final List<Supplier<Integer>> testSuppliers;
        private final Integer expectedResult;
        private int currentFunctionIndex = 0;

        public Assertion(String description, List<Supplier<Integer>> testSuppliers, Integer expectedResult) {
            this.description = description;
            this.testSuppliers = testSuppliers;
            this.expectedResult = expectedResult;
        }

        public String getDescription() {
            return description;
        }

        public List<Supplier<Integer>> getTestSuppliers() {
            return testSuppliers;
        }

        public Integer getExpectedResult() {
            return expectedResult;
        }

        public int getCurrentFunctionIndex() {
            return currentFunctionIndex;
        }

        public void incrementFunctionIndex() {
            currentFunctionIndex++;
        }

        public void resetFunctionIndex() {
            currentFunctionIndex = 0; // Reset the function index to 0
        }
    }
}
