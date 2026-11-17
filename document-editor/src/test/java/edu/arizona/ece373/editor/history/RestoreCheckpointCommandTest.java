package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link RestoreCheckpointCommand} (6.4.6).
 *
 * <p>Covers the main case, unknown names rejected by validate with the document
 * unchanged, boundaries (empty documents and empty paragraphs on either side of the
 * restore), and the undo round trip, including undoing edits made before and after
 * a restore.
 *
 * <p>A restore replaces the whole tree with copies, so these tests compare text and
 * titles rather than object identity.
 */
class RestoreCheckpointCommandTest {

    //main case

    @Test
    void restoresTheSavedText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "draft one").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        new InsertTextCommand(1, 9, " edited").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "draft");

        c.validate(d);
        c.execute(d);

        assertEquals("draft one", d.paragraph(1).text().plainText());
    }

    @Test
    void restoresTheSavedStructure() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("start", d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "b").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "start");

        c.validate(d);
        c.execute(d);

        assertEquals("One", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals("a", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
        assertThrows(EditException.class, () -> d.section(SectionPath.parse("2")));
        assertThrows(EditException.class, () -> d.section(SectionPath.parse("1.1")));
    }

    @Test
    void restoresTheNamedOneOfSeveralCheckpoints() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "v1").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("first", d);
        new InsertTextCommand(1, 2, "-v2").execute(d);
        store.save("second", d);
        new InsertTextCommand(1, 5, "-v3").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "first");

        c.validate(d);
        c.execute(d);

        assertEquals("v1", d.paragraph(1).text().plainText());
    }

    @Test
    void editsAfterTheRestoreDoNotChangeTheCheckpoint() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "saved").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);
        new RestoreCheckpointCommand(store, "a").execute(d);

        new InsertTextCommand(1, 0, "edited ").execute(d);
        new RestoreCheckpointCommand(store, "a").execute(d);

        assertEquals("saved", d.paragraph(1).text().plainText());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAnUnknownName() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "current").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "final");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("No checkpoint \"final\"; saved: draft", e.getMessage());
        assertEquals("current", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnyNameWhenNothingIsSaved() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "current").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(new CheckpointStore(), "draft");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("No checkpoint \"draft\"; no checkpoints saved", e.getMessage());
        assertEquals("current", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANullName() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, null);

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsANameThatDiffersOnlyInCase() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("Draft", d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "draft");

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class,
                () -> new RestoreCheckpointCommand(new CheckpointStore(), "x").validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedRestore() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "current").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new RestoreCheckpointCommand(new CheckpointStore(), "x")));

        assertFalse(h.canUndo());
        assertEquals("current", d.paragraph(1).text().plainText());
    }

    //boundaries: empty documents and empty paragraphs

    @Test
    void restoringAnEmptyDocumentClearsEverything() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("blank", d);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "text").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "blank");

        c.validate(d);
        c.execute(d);

        assertTrue(d.section(null).subsections().isEmpty());
        assertThrows(EditException.class, () -> d.paragraph(1));
    }

    @Test
    void restoringIntoAnEmptyDocument() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "saved").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("full", d);
        Document empty = new Document();
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "full");

        c.validate(empty);
        c.execute(empty);

        assertEquals("saved", empty.paragraph(1).text().plainText());
    }

    @Test
    void anEmptyParagraphIsRestoredAsEmpty() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);
        new InsertTextCommand(1, 0, "filled in").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "a");

        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
    }

    @Test
    void restoringTheCurrentStateChangesNothingVisible() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "same").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("now", d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "now");

        c.execute(d);

        assertEquals("same", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoGivesBackTheDocumentFromJustBeforeTheRestore() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "old").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("old", d);
        new InsertTextCommand(1, 3, " but newer").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "second").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "old");

        c.execute(d);
        c.undo(d);

        assertEquals("One", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals("Two", d.section(SectionPath.parse("2")).title().plainText());
        assertEquals("old but newer", d.paragraph(1).text().plainText());
        assertEquals("second", d.paragraph(2).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(3));
    }

    @Test
    void undoOfRestoringAnEmptyDocument() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("blank", d);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "text").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "blank");

        c.execute(d);
        c.undo(d);

        assertEquals("text", d.paragraph(1).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheDocumentUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "saved").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);
        new InsertTextCommand(1, 5, " then changed").execute(d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "a");

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("saved then changed", d.paragraph(1).text().plainText());
    }

    @Test
    void redoRestoresTheSameCheckpointEvenAfterItIsOverwritten() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "version A").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        new InsertTextCommand(1, 9, " edited").execute(d);
        History h = new History(d);

        h.perform(new RestoreCheckpointCommand(store, "draft")); // "version A"
        h.undo();                                                // "version A edited"
        store.save("draft", d);                                  // overwrite with the edited text
        h.redo();

        // Redo uses the checkpoint this command restored the first time.
        assertEquals("version A", d.paragraph(1).text().plainText());
    }

    @Test
    void editsBeforeAndAfterARestoreAllUndoThroughHistory() {
        Document d = new Document();
        History h = new History(d);
        CheckpointStore store = new CheckpointStore();
        h.perform(new AddSectionCommand(null, "S"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "draft one"));
        store.save("draft", d);
        h.perform(new InsertTextCommand(1, 9, " edited"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "more"));

        h.perform(new RestoreCheckpointCommand(store, "draft"));
        h.perform(new InsertTextCommand(1, 0, "X"));
        assertEquals("Xdraft one", d.paragraph(1).text().plainText());

        h.undo(); // the X
        h.undo(); // the restore
        assertEquals("draft one edited", d.paragraph(1).text().plainText());
        assertEquals("more", d.paragraph(2).text().plainText());

        h.undo(); // "more"
        h.undo(); // " edited"
        assertEquals("draft one", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));

        h.redo();
        h.redo();
        h.redo(); // the restore again
        assertEquals("draft one", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void deleteThenRestoreThenUndoBoth() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("hello", d);
        History h = new History(d);

        h.perform(new DeleteTextCommand(new TextRange(1, 5, 11)));  // "Hello"
        h.perform(new RestoreCheckpointCommand(store, "hello"));    // "Hello world"
        h.undo();                                                   // "Hello"
        assertEquals("Hello", d.paragraph(1).text().plainText());

        h.undo();                                                   // "Hello world"
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    // ---- events and describe ----

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "a");

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        RestoreCheckpointCommand c = new RestoreCheckpointCommand(store, "draft");

        assertEquals("restore checkpoint \"draft\"", c.describe());

        c.execute(d);

        assertEquals("restored checkpoint \"draft\"", c.describe());
    }
}
