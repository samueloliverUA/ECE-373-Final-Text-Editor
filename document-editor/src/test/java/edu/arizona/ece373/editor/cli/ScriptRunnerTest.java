package edu.arizona.ece373.editor.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.app.Editor;
import edu.arizona.ece373.editor.app.Result;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests {@link ScriptRunner}: blank and comment lines skipped, each command echoed, failing
 * lines reported by line number while the script continues, missing files, quit, recording
 * into macros, and identical output when the same script is run twice.
 *
 * <p>Satisfies 6.1.2, 6.9.7.
 */
class ScriptRunnerTest {

    @TempDir
    Path dir;

    private Editor editor;
    private ScriptRunner runner;
    private ByteArrayOutputStream buffer;
    private PrintStream out;

    @BeforeEach
    void setUp() {
        editor = new Editor();
        runner = new ScriptRunner(new CommandParser(), editor);
        buffer = new ByteArrayOutputStream();
        out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
    }

    private Path script(String name, String... lines) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return p;
    }

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /** Runs a script against a brand-new editor and returns everything it printed. */
    private static String runFresh(Path p) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream o = new PrintStream(b, true, StandardCharsets.UTF_8);
        Result r = new ScriptRunner(new CommandParser(), new Editor()).run(p, o);
        o.println(r.message);
        return b.toString(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------------------------------
    // Skipping and echoing
    // ---------------------------------------------------------------------------------------

    @Test
    void blankAndCommentLinesSkippedAndCommandsEchoed() throws IOException {
        Path p = script("s.txt",
                "# a comment",
                "",
                "section add \"Intro\"",
                "   ",
                "   # indented comment",
                "para add 1 \"hello\"");
        Result r = runner.run(p, out);
        assertTrue(r.ok, r.message);
        assertEquals(String.join("\n",
                "> section add \"Intro\"",
                "Added section 1 \"Intro\"",
                "> para add 1 \"hello\"",
                "Added paragraph 1 to section 1",
                ""), output());
        assertEquals("Ran s.txt (6 lines, 0 errors)", r.message);
    }

    @Test
    void echoedCommandIsStripped() throws IOException {
        runner.run(script("s.txt", "    outline   "), out);
        assertTrue(output().startsWith("> outline\n"), output());
    }

    @Test
    void commandsActuallyRunAgainstTheEditor() throws IOException {
        runner.run(script("s.txt",
                "section add \"Intro\"",
                "para add 1 \"one two\"",
                "format p1:0-3 bold on"), out);
        assertEquals("one two", editor.document().paragraph(1).text().plainText());
        assertTrue(editor.document().paragraph(1).text().formatAt(0).bold);
    }

    @Test
    void emptyAndCommentOnlyScripts() throws IOException {
        Path empty = dir.resolve("e.txt");
        Files.writeString(empty, "");
        assertEquals("Ran e.txt (0 lines, 0 errors)", runner.run(empty, out).message);
        assertEquals("", output());
        Result r = runner.run(script("c.txt", "# only", "# comments"), out);
        assertTrue(r.ok);
        assertEquals("", output());
    }

    // ---------------------------------------------------------------------------------------
    // Failing lines
    // ---------------------------------------------------------------------------------------

    @Test
    void failingLineReportsLineNumberAndScriptContinues() throws IOException {
        Path p = script("s.txt",
                "# line 1 is a comment",
                "section add \"Intro\"",
                "para add 9 \"nowhere\"",        // line 3: no section 9
                "",
                "bogus command",                 // line 5: parse error
                "para add 1 \"after errors\"");  // line 6: still runs
        Result r = runner.run(p, out);
        assertTrue(r.ok, "a script with failing lines still completes");
        String o = output();
        assertTrue(o.contains("> para add 9 \"nowhere\"\nline 3: Error: No section 9"), o);
        assertTrue(o.contains("> bogus command\nline 5: Error: unknown command 'bogus'"), o);
        assertTrue(o.contains("> para add 1 \"after errors\"\nAdded paragraph 1"), o);
        assertEquals("Ran s.txt (6 lines, 2 errors)", r.message);
        assertEquals("after errors", editor.document().paragraph(1).text().plainText());
    }

    @Test
    void oneErrorIsSingular() throws IOException {
        Result r = runner.run(script("s.txt", "undo", "width 5"), out);
        assertEquals("Ran s.txt (2 lines, 1 error)", r.message);
    }

    @Test
    void failingLineLeavesDocumentUnchanged() throws IOException {
        runner.run(script("s.txt",
                "section add \"Intro\"",
                "para add 1 \"abc\"",
                "delete p1:0-99"), out);
        assertEquals("abc", editor.document().paragraph(1).text().plainText());
    }

    // ---------------------------------------------------------------------------------------
    // Missing file and quit
    // ---------------------------------------------------------------------------------------

    @Test
    void missingFileIsError() {
        Result r = runner.run(dir.resolve("nope.txt"), out);
        assertFalse(r.ok);
        assertTrue(r.message.startsWith("No such script file:"), r.message);
        assertEquals("", output());
    }

    @Test
    void directoryIsNotAScript() {
        Result r = runner.run(dir, out);
        assertFalse(r.ok);
    }

    @Test
    void quitEndsScriptEarly() throws IOException {
        Result r = runner.run(script("s.txt",
                "section add \"Intro\"",
                "quit",
                "section add \"Never\""), out);
        assertTrue(r.quit);
        assertFalse(output().contains("Never\"\nAdded"), output());
        assertEquals(1, editor.document().root().subsections().size());
    }

    // ---------------------------------------------------------------------------------------
    // Repeatability (6.9.7)
    // ---------------------------------------------------------------------------------------

    @Test
    void sameScriptTwiceGivesIdenticalOutput() throws IOException {
        Path p = script("demo.txt",
                "# demo",
                "section add \"Intro\"",
                "para add 1 \"the cat sat on the mat with another cat\"",
                "format p1:4-7 bold on",
                "checkpoint save one",
                "replace-all \"cat\" \"dog\"",
                "stats",
                "find \"dog\"",
                "view markup",
                "width 20",
                "breaker balanced",
                "show",
                "undo",
                "checkpoint restore one",
                "delete p1:0-999",
                "macro record m",
                "insert p1:0 \"A \"",
                "macro end",
                "macro play m",
                "outline");
        String first = runFresh(p);
        String second = runFresh(p);
        assertEquals(first, second);
        assertTrue(first.contains("line 15: Error:"), first);
        assertTrue(first.contains("Found 2: p1:4-7, p1:36-39"), first);
    }

    @Test
    void linesRunInsideARecordingAreRecorded() throws IOException {
        editor.startMacro("m");
        runner.run(script("s.txt", "section add \"A\"", "undo", "section add \"B\""), out);
        editor.endMacro();
        // two section adds recorded; the undo is not
        assertEquals(2, editor.macros().size("m"));
    }
}
