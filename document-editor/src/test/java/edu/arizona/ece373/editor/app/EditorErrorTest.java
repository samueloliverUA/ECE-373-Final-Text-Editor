package edu.arizona.ece373.editor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import edu.arizona.ece373.editor.cli.CommandParser;
import edu.arizona.ece373.editor.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests error handling end to end: every kind of invalid edit, typed as a command, returns
 * {@code Result.error}, leaves the document byte-for-byte unchanged (render, markup render,
 * outline and stats compared before and after) and adds nothing to the history. No exception
 * ever escapes the Editor.
 *
 * <p>Satisfies 6.1.4.
 */
class EditorErrorTest {

    private Editor editor;
    private final CommandParser parser = new CommandParser();

    /** Section 1 with p1 "hello world", section 1.1 with p2 "second". */
    @BeforeEach
    void setUp() {
        editor = new Editor();
        for (String line : new String[] {
            "section add \"Intro\"",
            "para add 1 \"hello world\"",
            "section add 1 \"Sub\"",
            "para add 1.1 \"second\"",
            "format p1:0-5 bold on",
            "checkpoint save base"}) {
            Result r = run(line);
            if (!r.ok) {
                throw new AssertionError(line + ": " + r.message);
            }
        }
    }

    private Result run(String line) {
        try {
            return editor.run(parser.parse(line));
        } catch (ParseException e) {
            throw new AssertionError("should parse: " + line + " (" + e.getMessage() + ")");
        }
    }

    /** Everything visible about the document. */
    private String snapshot() {
        editor.view().setView("plain");
        String plain = editor.render();
        editor.view().setView("markup");
        String markup = editor.render();
        editor.view().setView("plain");
        return plain + "\n--\n" + markup + "\n--\n" + editor.document().outline()
                + "\n--\n" + editor.stats();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        // structure
        "section add 7 \"Nope\"",
        "section add 1.4 \"Nope\"",
        "section add \"   \"",
        "para add 3 \"x\"",
        "para add 1.1.1 \"x\"",
        // editing
        "insert p9:0 \"x\"",
        "insert p1:12 \"x\"",
        "delete p1:0-99",
        "delete p3:0-1",
        "replace p1:5-40 \"x\"",
        "replace p4:0-1 \"x\"",
        "move p1:0-5 p1:2",
        "move p1:0-5 p9:0",
        "split p1:50",
        "split p7:0",
        "merge p2",
        "merge p9",
        // formatting
        "format p1:0-50 bold on",
        "format p8:0-1 italic on",
        "format p1:0-5 size 5",
        "format p1:0-5 size 73",
        "format p1:0-5 color reddish",
        "format title 4 bold on",
        "format title 1.2 size 20",
        "format title 1 size 100",
        // history and checkpoints
        "checkpoint restore missing",
        // search
        "find \"\"",
        "replace-all \"\" \"x\"",
        "replace-first \"\" \"x\"",
        // macros
        "macro play nothing",
        "macro end",
        // viewing
        "width 9",
        "width 201",
        // scripts
        "run \"does-not-exist.txt\""
    })
    void invalidCommandIsErrorAndChangesNothing(String line) {
        String before = snapshot();
        Result r = run(line);
        assertFalse(r.ok, line + " should fail but said: " + r.message);
        assertFalse(r.message.isBlank(), line);
        assertFalse(r.message.contains("internal error"), line + ": " + r.message);
        assertEquals(before, snapshot(), line + " changed the document");
        // Nothing was recorded: the last undo entry is still the format from setUp.
        Result undo = editor.undo();
        assertEquals("Undid: formatted p1:0-5 bold on", undo.message, line);
    }

    @ParameterizedTest
    @ValueSource(strings = {"width 9", "width 201"})
    void badWidthKeepsOldWidth(String line) {
        run("width 40");
        assertFalse(run(line).ok);
        assertEquals(40, editor.view().width());
    }
}
