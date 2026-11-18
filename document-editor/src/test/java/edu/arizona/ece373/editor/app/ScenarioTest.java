package edu.arizona.ece373.editor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import edu.arizona.ece373.editor.cli.CommandParser;
import edu.arizona.ece373.editor.cli.ScriptRunner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Runs the ConOps Section 2.1 scenario end to end: {@code scenario.txt} through
 * {@link ScriptRunner} on a fresh {@link Editor}, compared byte for byte with
 * {@code scenario-expected.txt}. The output is exactly what
 * {@code java -jar editor.jar scenario.txt} prints: every echoed command and result, then
 * the summary line.
 *
 * <p>This is the team's progress check. When output wording changes on purpose, rerun the
 * script, check the new lines by hand and save them:
 * <pre>
 *   mvn -q compile
 *   java -cp target/classes edu.arizona.ece373.editor.cli.ConsoleApp \
 *       src/test/resources/scenario.txt &gt; src/test/resources/scenario-expected.txt
 * </pre>
 * Line endings are normalised so the test also passes on a Windows checkout.
 */
class ScenarioTest {

    private static Path resource(String name) throws URISyntaxException {
        URL url = ScenarioTest.class.getResource("/" + name);
        if (url == null) {
            fail("missing test resource " + name);
        }
        return Path.of(url.toURI());
    }

    /** Runs the script exactly as ConsoleApp.runScript does and returns everything printed. */
    private static String runScenario() throws URISyntaxException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        Result r = new ScriptRunner(new CommandParser(), new Editor())
                .run(resource("scenario.txt"), out);
        assertTrue(r.ok, r.message);
        out.println(r.message);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String normalise(String s) {
        return s.replace("\r\n", "\n");
    }

    @Test
    void scenarioMatchesExpectedOutput() throws IOException, URISyntaxException {
        String expected = normalise(Files.readString(resource("scenario-expected.txt"),
                StandardCharsets.UTF_8));
        String actual = normalise(runScenario());
        if (!expected.equals(actual)) {
            fail(firstDifference(expected, actual));
        }
    }

    @Test
    void scenarioIsRepeatable() throws URISyntaxException {
        assertEquals(runScenario(), runScenario());
    }

    @Test
    void onlyTheMarkedStepsFail() throws URISyntaxException {
        String out = runScenario();
        // Ten alternate-flow steps are expected to fail; anything else is a regression.
        assertTrue(out.strip().endsWith("10 errors)"), out.lines().reduce((a, b) -> b)
                .orElse(""));
        assertTrue(!out.contains("internal error"), "an exception escaped the editor");
        assertTrue(!out.contains("not available yet"), "a verb is not wired up");
    }

    /** A short message pointing at the first line that differs, instead of a huge dump. */
    private static String firstDifference(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);
        int n = Math.min(e.length, a.length);
        for (int i = 0; i < n; i++) {
            if (!e[i].equals(a[i])) {
                return "scenario output differs at line " + (i + 1)
                        + "\n  expected: " + e[i]
                        + "\n  actual:   " + a[i]
                        + "\n(if the change is intended, regenerate scenario-expected.txt;"
                        + " see the class comment)";
            }
        }
        return "scenario output has " + a.length + " lines, expected " + e.length;
    }
}
