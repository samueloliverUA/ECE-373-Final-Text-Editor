package edu.arizona.ece373.editor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.cli.CommandParser;
import edu.arizona.ece373.editor.cli.ParseException;
import edu.arizona.ece373.editor.model.EditException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MacroLibrary} through the Editor, the way the console uses it: record, end,
 * play in order; one undo reverses a whole replay and redo re-applies it; a failing step
 * rolls the replay back and is reported; errors for unknown names, nested play and recording
 * twice; undo/redo/macro/run are not recorded.
 *
 * <p>Satisfies 6.8.5, 6.8.6, 6.8.7.
 */
class MacroLibraryTest {

    private Editor editor;
    private final CommandParser parser = new CommandParser();

    @BeforeEach
    void setUp() {
        editor = new Editor();
        ok("section add \"S\"");
        ok("para add 1 \"one two three\"");
    }

    /** Runs a line exactly as the console does. */
    private Result run(String line) {
        try {
            return editor.run(parser.parse(line));
        } catch (ParseException e) {
            throw new AssertionError(line + ": " + e.getMessage());
        }
    }

    private Result ok(String line) {
        Result r = run(line);
        assertTrue(r.ok, line + " -> " + r.message);
        return r;
    }

    private Result error(String line) {
        Result r = run(line);
        assertFalse(r.ok, line + " -> " + r.message);
        return r;
    }

    private String text() {
        return editor.document().paragraph(1).text().plainText();
    }

    /** Records "tidy": insert "X" at the start, then replace-all "two" with "2". */
    private void recordTidy() {
        ok("macro record tidy");
        ok("insert p1:0 \"X\"");
        ok("replace-all \"two\" \"2\"");
        assertEquals("Recorded macro \"tidy\" (2 steps)", ok("macro end").message);
    }

    // ---------------------------------------------------------------------------------------
    // Record, end, play
    // ---------------------------------------------------------------------------------------

    @Test
    void recordingAlsoRunsTheSteps() {
        recordTidy();
        assertEquals("Xone 2 three", text());
        assertEquals(List.of("tidy"), editor.macros().names());
        assertEquals(2, editor.macros().size("tidy"));
    }

    @Test
    void playRunsStepsInOrder() {
        recordTidy();
        ok("para add 1 \"two\"");
        assertEquals("Played macro \"tidy\" (2 steps)", ok("macro play tidy").message);
        // insert X into p1, then replace "two" everywhere (p2 included)
        assertEquals("XXone 2 three", text());
        assertEquals("2", editor.document().paragraph(2).text().plainText());
    }

    @Test
    void stepsAreReEvaluatedOnReplay() {
        ok("macro record r");
        ok("replace-all \"a\" \"b\"");
        ok("macro end");
        ok("para add 1 \"banana\"");
        ok("macro play r");
        assertEquals("bbnbnb", editor.document().paragraph(2).text().plainText());
    }

    @Test
    void recordingOverwritesMacroWithSameName() {
        recordTidy();
        ok("macro record tidy");
        ok("delete p1:0-1");
        ok("macro end");
        assertEquals(1, editor.macros().size("tidy"));
    }

    @Test
    void emptyMacroPlaysAndRecordsNothing() {
        ok("macro record empty");
        assertEquals("Recorded macro \"empty\" (0 steps)", ok("macro end").message);
        ok("macro play empty");
        // The last history entry is still the paragraph add.
        assertTrue(ok("undo").message.contains("paragraph"));
    }

    @Test
    void failedStepsAreNotRecorded() {
        ok("macro record m");
        error("delete p1:0-99");
        ok("delete p1:0-4");
        ok("macro end");
        assertEquals(1, editor.macros().size("m"));
    }

    @Test
    void nonEditStepsAreReplayedToo() {
        ok("macro record v");
        ok("view markup");
        ok("width 30");
        ok("macro end");
        ok("view plain");
        ok("width 80");
        ok("macro play v");
        assertEquals(30, editor.view().width());
        assertTrue(editor.show().message.contains("one two three"));
    }

    // ---------------------------------------------------------------------------------------
    // Undo and redo of a replay
    // ---------------------------------------------------------------------------------------

    @Test
    void oneUndoRestoresDocumentExactlyAsBeforeReplay() {
        recordTidy();
        ok("para add 1 \"two two\"");
        ok("format p2:0-3 bold on");
        ok("view markup");
        String before = editor.render();
        ok("macro play tidy");
        assertFalse(before.equals(editor.render()));
        assertEquals("Undid: macro tidy (2)", ok("undo").message);
        assertEquals(before, editor.render());
    }

    @Test
    void redoReappliesWholeReplay() {
        recordTidy();
        ok("macro play tidy");
        String after = editor.render();
        ok("undo");
        ok("redo");
        assertEquals(after, editor.render());
        ok("undo");
        assertEquals("Xone 2 three", text());
    }

    @Test
    void replayIsOneHistoryEntry() {
        recordTidy();
        ok("macro play tidy");
        ok("undo");                       // the whole replay
        assertEquals("Xone 2 three", text());
        ok("undo");                       // replace-all from recording
        assertEquals("Xone two three", text());
    }

    // ---------------------------------------------------------------------------------------
    // Failing step
    // ---------------------------------------------------------------------------------------

    @Test
    void failingStepRollsBackReplayAndIsReported() {
        ok("macro record cut");
        ok("insert p1:0 \"AB\"");         // step 1: always works -> "ABone two three"
        ok("delete p1:10-15");            // step 2: needs 15 characters -> "ABone two "
        ok("macro end");
        ok("delete p1:0-6");              // "two " (4): on replay step 1 makes it 6, so
                                          // step 2 fails
        String before = editor.render();

        Result r = error("macro play cut");
        assertTrue(r.message.contains("step 2 of 2"), r.message);
        assertTrue(r.message.contains("rolled back"), r.message);
        assertEquals(before, editor.render());
        assertFalse(editor.history().isGroupOpen());
        // Nothing from the failed replay is in the history.
        assertEquals("two ", text());
        ok("undo");                       // undoes "delete p1:0-6", not the replay
        assertEquals("ABone two ", text());
    }

    // ---------------------------------------------------------------------------------------
    // Errors
    // ---------------------------------------------------------------------------------------

    @Test
    void unknownNameIsError() {
        assertTrue(error("macro play nope").message.contains("no macros recorded"));
        recordTidy();
        assertTrue(error("macro play nope").message.contains("recorded: tidy"));
    }

    @Test
    void recordWhileRecordingIsError() {
        ok("macro record a");
        assertTrue(error("macro record b").message.contains("Already recording"));
        assertTrue(editor.macros().isRecording());
    }

    @Test
    void endWithoutRecordingIsError() {
        assertTrue(error("macro end").message.contains("Not recording"));
    }

    @Test
    void nestedPlayIsError() {
        MacroLibrary lib = editor.macros();
        lib.startRecording("inner");
        lib.stopRecording();
        lib.startRecording("outer");
        lib.record(e -> lib.play("inner", e));   // a step that tries to play a macro
        lib.stopRecording();
        Result r = lib.play("outer", editor);
        assertFalse(r.ok);
        assertTrue(r.message.contains("another is playing"), r.message);
        assertFalse(lib.isPlaying());
        assertFalse(editor.history().isGroupOpen());
    }

    @Test
    void blankNameIsError() {
        assertThrows(EditException.class, () -> editor.macros().startRecording(" "));
    }

    // ---------------------------------------------------------------------------------------
    // Not recorded
    // ---------------------------------------------------------------------------------------

    @Test
    void undoRedoMacroRunAndQuitAreNotRecorded() {
        recordTidy();
        ok("macro record m");
        ok("insert p1:0 \"Q\"");
        ok("undo");
        ok("redo");
        ok("macro play tidy");
        error("run \"no-such-file.txt\"");
        ok("quit");
        ok("macro end");
        assertEquals(1, editor.macros().size("m"));
        for (String verb : List.of("undo", "redo", "macro end", "macro play x",
                "macro record x", "run \"f\"", "quit")) {
            try {
                assertFalse(parser.parse(verb).recordable(), verb);
            } catch (ParseException e) {
                throw new AssertionError(e);
            }
        }
    }
}
