package com.example.APITestingPlugin;

import com.example.APITestingPlugin.enums.APITests;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "<html><font color=#86C43F>[RB]</font> Testing Plugin</html>",
        description = "API Testing Plugin",
        enabledByDefault = false,
        tags = {"ethan"}
)
public class APITestingPlugin extends Plugin {
    private APITests currentTest;
    private int currentAssertionIndex = 0;
    private final List<TestResult> testResults = new ArrayList<>();

    @Override
    public void startUp() throws Exception {
        log.info("APITestingPlugin started!");
        // Reset the current test and assertion index
        currentTest = null;
        currentAssertionIndex = 0;
        testResults.clear();

        // Reset all the assertions
        for (APITests test : APITests.values()) {
            for (APITests.Assertion assertion : test.getAssertions()) {
                assertion.resetFunctionIndex();
            }
        }
    }

    @Override
    public void shutDown() throws Exception {
        log.info("APITestingPlugin stopped!");
        generateHtmlReport();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        System.out.println("Game tick");
        System.out.println("Current test: " + currentTest);
        System.out.println("Current assertion index: " + currentAssertionIndex);

        if (currentTest == null || currentAssertionIndex >= currentTest.getAssertions().size()) {
            currentTest = APITests.values()[0];
            currentAssertionIndex = 0;
            return;
        }

        APITests.Assertion currentAssertion = currentTest.getAssertions().get(currentAssertionIndex);
        if (currentAssertion.getCurrentFunctionIndex() < currentAssertion.getTestSuppliers().size()) {
            Supplier<Integer> function = currentAssertion.getTestSuppliers().get(currentAssertion.getCurrentFunctionIndex());
            Object result = function.get();
            currentAssertion.incrementFunctionIndex();

            if (currentAssertion.getCurrentFunctionIndex() == currentAssertion.getTestSuppliers().size()) {
                boolean passed = currentAssertion.getExpectedResult().equals(result);
                testResults.add(new TestResult(currentTest.getTestName(), currentAssertion.getDescription(), currentAssertion.getExpectedResult(), result, passed));
                currentAssertionIndex++;
            }
        }
    }

    private void generateHtmlReport() {
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<!DOCTYPE html><html><head>")
                .append("<title>API Test Results</title>")
                .append("<link href=\"https://cdn.jsdelivr.net/npm/tailwindcss@2.2.19/dist/tailwind.min.css\" rel=\"stylesheet\">")
                .append("</head><body class=\"bg-gray-100 p-4\">")
                .append("<div class=\"container mx-auto\">")
                .append("<h1 class=\"text-2xl font-bold mb-4\">API Test Results</h1>")
                .append("<table class=\"table-auto w-full border-collapse border border-gray-200\">")
                .append("<thead><tr class=\"bg-gray-200\">")
                .append("<th class=\"border border-gray-300 px-4 py-2\">Test Name</th>")
                .append("<th class=\"border border-gray-300 px-4 py-2\">Assertion Description</th>")
                .append("<th class=\"border border-gray-300 px-4 py-2\">Expected Result</th>")
                .append("<th class=\"border border-gray-300 px-4 py-2\">Actual Result</th>")
                .append("<th class=\"border border-gray-300 px-4 py-2\">Result</th>")
                .append("</tr></thead><tbody>");

        for (TestResult result : testResults) {
            String resultColor = result.passed ? "bg-green-100" : "bg-red-100";
            htmlContent.append("<tr class=\"").append(resultColor).append("\">")
                    .append("<td class=\"border border-gray-300 px-4 py-2\">").append(result.testName).append("</td>")
                    .append("<td class=\"border border-gray-300 px-4 py-2\">").append(result.assertionDescription).append("</td>")
                    .append("<td class=\"border border-gray-300 px-4 py-2\">").append(result.expectedResult).append("</td>")
                    .append("<td class=\"border border-gray-300 px-4 py-2\">").append(result.actualResult).append("</td>")
                    .append("<td class=\"border border-gray-300 px-4 py-2 font-bold\">").append(result.passed ? "PASS" : "FAIL").append("</td>")
                    .append("</tr>");
        }

        htmlContent.append("</tbody></table></div></body></html>");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("APITestResults.html"))) {
            writer.write(htmlContent.toString());
            // Open the HTML report in the default browser
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + "APITestResults.html");
        } catch (IOException e) {
            log.error("Error writing HTML report", e);
        }
    }


    private static class TestResult {
        private final String testName;
        private final String assertionDescription;
        private final Object expectedResult;
        private final Object actualResult;
        private final boolean passed;

        public TestResult(String testName, String assertionDescription, Object expectedResult, Object actualResult, boolean passed) {
            this.testName = testName;
            this.assertionDescription = assertionDescription;
            this.expectedResult = expectedResult;
            this.actualResult = actualResult;
            this.passed = passed;
        }
    }
}
