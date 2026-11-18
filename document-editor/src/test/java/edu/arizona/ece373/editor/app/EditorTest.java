package edu.arizona.ece373.editor.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.InsertTextCommand;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.search.SubstringMatch;
import edu.arizona.ece373.editor.search.WholeWordMatch;
import edu.arizona.ece373.editor.view.GreedyLineBreaker;
import edu.arizona.ece373.editor.view.PlainTextRenderer;
import edu.arizona.ece373.editor.view.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link Editor} facade: a fresh editor, perform/undo/redo round trips, stats and
 * render reflecting the latest change, checkpoints, view settings, search and replace.
 */
class EditorTest {

    private Editor editor;

    @BeforeEach
    void setUp() {
        editor = new Editor();
    }

    private void ok(Result r) {
        assertTrue(r.ok, r.message);
    }

    private void build() {
        ok(editor.perform(new AddSectionCommand(null, "Intro")));
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "the cat sat")));
    }

    private String p1() {
        return editor.document().paragraph(1).text().plainText();
    }

    // ---------------------------------------------------------------------------------------
    // Fresh editor
    // ---------------------------------------------------------------------------------------

    @Test
    void freshEditorHasEmptyDocumentAndEveryCollaborator() {
        assertTrue(editor.document().root().children().isEmpty());
        assertEquals("(empty document)", editor.render());
        assertNotNull(editor.history());
        assertNotNull(editor.checkpoints());
        assertNotNull(editor.macros());
        assertNotNull(editor.search());
        assertInstanceOf(PlainTextRenderer.class, editor.view().renderer());
        assertInstanceOf(GreedyLineBreaker.class, editor.view().breaker());
        assertEquals(80, editor.view().width());
        assertEquals(new Statistics(0, 0, 0), editor.stats());
        assertFalse(editor.history().canUndo());
        assertTrue(editor.checkpoints().names().isEmpty());
        assertTrue(editor.macros().names().isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // perform / undo / redo
    // ---------------------------------------------------------------------------------------

    @Test
    void performUndoRedoRoundTrip() {
        build();
        Result r = editor.perform(new InsertTextCommand(1, 0, "Look, "));
        ok(r);
        assertEquals("Look, the cat sat", p1());
        assertTrue(Character.isUpperCase(r.message.charAt(0)), r.message);

        Result u = editor.undo();
        ok(u);
        assertTrue(u.message.startsWith("Undid: "), u.message);
        assertEquals("the cat sat", p1());

        Result rd = editor.redo();
        ok(rd);
        assertTrue(rd.message.startsWith("Redid: "), rd.message);
        assertEquals("Look, the cat sat", p1());
    }

    @Test
    void undoAndRedoWithNothingToDo() {
        assertEquals("Nothing to undo", editor.undo().message);
        assertEquals("Nothing to redo", editor.redo().message);
    }

    // ---------------------------------------------------------------------------------------
    // stats and render follow the latest change
    // ---------------------------------------------------------------------------------------

    @Test
    void statsReflectLatestChangeIncludingUndo() {
        build();
        assertEquals(new Statistics(3, 11, 1), editor.stats());
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "two words")));
        assertEquals(new Statistics(5, 20, 2), editor.stats());
        ok(editor.undo());
        assertEquals(new Statistics(3, 11, 1), editor.stats());
        ok(editor.redo());
        assertEquals(new Statistics(5, 20, 2), editor.stats());
    }

    @Test
    void statsFollowCheckpointRestore() {
        build();
        ok(editor.saveCheckpoint("one"));
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "more")));
        assertEquals(2, editor.stats().paragraphs());
        ok(editor.restoreCheckpoint("one"));
        assertEquals(1, editor.stats().paragraphs());
    }

    @Test
    void renderReflectsLatestChange() {
        build();
        assertEquals("1 Intro\n\nthe cat sat", editor.render());
        ok(editor.perform(new InsertTextCommand(1, 11, " down")));
        assertEquals("1 Intro\n\nthe cat sat down", editor.render());
        assertEquals(editor.render(), editor.show().message);
    }

    // ---------------------------------------------------------------------------------------
    // Checkpoints
    // ---------------------------------------------------------------------------------------

    @Test
    void checkpointSaveListRestoreAndUndo() {
        assertEquals("No checkpoints saved", editor.listCheckpoints().message);
        build();
        ok(editor.saveCheckpoint("b"));
        ok(editor.saveCheckpoint("a"));
        assertEquals("Checkpoints: a, b", editor.listCheckpoints().message);
        ok(editor.perform(new InsertTextCommand(1, 0, "X")));
        ok(editor.restoreCheckpoint("a"));
        assertEquals("the cat sat", p1());
        ok(editor.undo());
        assertEquals("Xthe cat sat", p1());
    }

    // ---------------------------------------------------------------------------------------
    // View settings
    // ---------------------------------------------------------------------------------------

    @Test
    void viewWidthAndBreakerChangeOutputAndAreNotUndoable() {
        build();
        ok(editor.setWidth(10));
        assertEquals("1 Intro\n\nthe cat\nsat", editor.render());
        ok(editor.setBreaker("balanced"));
        ok(editor.setView("markup"));
        assertEquals("View: markup", editor.setView("markup").message);
        // Undo affects the document, not the view.
        ok(editor.undo());
        assertEquals(10, editor.view().width());
    }

    // ---------------------------------------------------------------------------------------
    // Search and replace
    // ---------------------------------------------------------------------------------------

    @Test
    void findListsMatchesInDocumentOrder() {
        build();
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "a cat")));
        assertEquals("Found 2: p1:4-7, p2:2-5",
                editor.find("cat", new SubstringMatch()).message);
        assertEquals("No matches for \"ca\"",
                editor.find("ca", new WholeWordMatch()).message);
    }

    @Test
    void replaceAllIsOneUndo() {
        build();
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "cat cat")));
        ok(editor.replaceAll("cat", "dog", new SubstringMatch()));
        assertEquals("the dog sat", p1());
        assertEquals("dog dog", editor.document().paragraph(2).text().plainText());
        ok(editor.undo());
        assertEquals("the cat sat", p1());
        assertEquals("cat cat", editor.document().paragraph(2).text().plainText());
    }

    @Test
    void replaceFirstReplacesOnlyFirst() {
        build();
        ok(editor.perform(new AddParagraphCommand(SectionPath.parse("1"), "cat")));
        ok(editor.replaceFirst("cat", "dog", new SubstringMatch()));
        assertEquals("the dog sat", p1());
        assertEquals("cat", editor.document().paragraph(2).text().plainText());
    }

    @Test
    void replaceWithNoMatchesRecordsNothing() {
        build();
        Result r = editor.replaceAll("zebra", "x", new SubstringMatch());
        ok(r);
        assertTrue(r.message.contains("nothing replaced"), r.message);
        r = editor.replaceFirst("zebra", "x", new SubstringMatch());
        assertTrue(r.message.contains("nothing replaced"), r.message);
        // The last entry is still the paragraph add.
        assertTrue(editor.undo().message.contains("paragraph"));
    }

    // ---------------------------------------------------------------------------------------
    // run()
    // ---------------------------------------------------------------------------------------

    @Test
    void runRecordsOnlySuccessfulRecordableActions() {
        build();
        ok(editor.startMacro("m"));
        ok(editor.run(e -> e.perform(new InsertTextCommand(1, 0, "A"))));
        assertFalse(editor.run(e -> e.perform(new InsertTextCommand(9, 0, "B"))).ok);
        ok(editor.run(new Action() {
            @Override
            public Result run(Editor e) {
                return e.undo();
            }

            @Override
            public boolean recordable() {
                return false;
            }
        }));
        ok(editor.endMacro());
        assertEquals(1, editor.macros().size("m"));
    }

    @Test
    void runTurnsEscapingExceptionsIntoErrors() {
        Result r = editor.run(e -> {
            throw new IllegalStateException("boom");
        });
        assertFalse(r.ok);
        assertEquals("boom", r.message);
    }
}
