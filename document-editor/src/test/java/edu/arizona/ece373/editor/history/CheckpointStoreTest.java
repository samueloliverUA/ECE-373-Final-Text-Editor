package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link CheckpointStore} and the {@code DocumentMemento} it holds.
 *
 * <p>Covers save then restore, edits after a save not changing the checkpoint,
 * overwriting a name, unknown names listing the saved names, and sorted names.
 * A checkpoint's contents are read by restoring it into a fresh Document.
 */
class CheckpointStoreTest {

    //save, edit, restore gives the saved state

    @Test
    void saveEditRestoreGivesTheSavedState() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "saved text").execute(d);
        CheckpointStore store = new CheckpointStore();

        store.save("draft", d);
        new InsertTextCommand(1, 0, "changed ").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "extra").execute(d);
        d.restore(store.get("draft"));

        assertEquals("saved text", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void restoreBringsBackSectionsAndTitles() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        CheckpointStore store = new CheckpointStore();

        store.save("outline", d);
        new AddSectionCommand(null, "Three").execute(d);
        d.restore(store.get("outline"));

        assertEquals("One", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals("One.One", d.section(SectionPath.parse("1.1")).title().plainText());
        assertEquals("Two", d.section(SectionPath.parse("2")).title().plainText());
        assertThrows(EditException.class, () -> d.section(SectionPath.parse("3")));
    }

    //editing after save does not change the checkpoint

    @Test
    void textEditsAfterSaveDoNotChangeTheCheckpoint() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "original").execute(d);
        CheckpointStore store = new CheckpointStore();

        store.save("a", d);
        new DeleteTextCommand(new TextRange(1, 0, 4)).execute(d);

        Document check = new Document();
        check.restore(store.get("a"));
        assertEquals("original", check.paragraph(1).text().plainText());
    }

    @Test
    void structureEditsAfterSaveDoNotChangeTheCheckpoint() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "only").execute(d);
        CheckpointStore store = new CheckpointStore();

        store.save("a", d);
        new AddParagraphCommand(SectionPath.parse("1"), "added later").execute(d);
        new AddSectionCommand(null, "Later").execute(d);

        Document check = new Document();
        check.restore(store.get("a"));
        assertEquals("only", check.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> check.paragraph(2));
        assertThrows(EditException.class, () -> check.section(SectionPath.parse("2")));
    }

    @Test
    void editsAfterARestoreDoNotChangeTheCheckpoint() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "keep me").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);

        d.restore(store.get("a"));
        new InsertTextCommand(1, 0, "edited ").execute(d);
        d.restore(store.get("a"));

        // Restoring twice gives the saved state both times.
        assertEquals("keep me", d.paragraph(1).text().plainText());
    }

    @Test
    void checkpointsAreIndependentOfEachOther() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "v1").execute(d);
        CheckpointStore store = new CheckpointStore();

        store.save("first", d);
        new InsertTextCommand(1, 2, "-v2").execute(d);
        store.save("second", d);

        d.restore(store.get("first"));
        assertEquals("v1", d.paragraph(1).text().plainText());

        d.restore(store.get("second"));
        assertEquals("v1-v2", d.paragraph(1).text().plainText());
    }

    @Test
    void anEmptyDocumentCanBeSavedAndRestored() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("blank", d);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "text").execute(d);

        d.restore(store.get("blank"));

        assertTrue(d.section(null).subsections().isEmpty());
        assertThrows(EditException.class, () -> d.paragraph(1));
    }

    //overwrite an existing name

    @Test
    void savingAnExistingNameOverwritesIt() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "old").execute(d);
        CheckpointStore store = new CheckpointStore();
        store.save("draft", d);
        new InsertTextCommand(1, 3, " and new").execute(d);

        store.save("draft", d);
        d.restore(store.get("draft"));

        assertEquals("old and new", d.paragraph(1).text().plainText());
        assertEquals(List.of("draft"), store.names());
    }

    //unknown name lists the saved names

    @Test
    void unknownNameListsTheSavedNames() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("beta", d);
        store.save("alpha", d);

        EditException e = assertThrows(EditException.class, () -> store.get("gamma"));

        assertEquals("No checkpoint \"gamma\"; saved: alpha, beta", e.getMessage());
    }

    @Test
    void unknownNameWithNothingSaved() {
        CheckpointStore store = new CheckpointStore();

        EditException e = assertThrows(EditException.class, () -> store.get("draft"));

        assertEquals("No checkpoint \"draft\"; no checkpoints saved", e.getMessage());
    }

    @Test
    void namesAreCaseSensitive() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("Draft", d);

        assertThrows(EditException.class, () -> store.get("draft"));
        assertTrue(store.has("Draft"));
        assertFalse(store.has("draft"));
    }

    //bad names

    @Test
    void nullNameIsUnknownNotACrash() {
        CheckpointStore store = new CheckpointStore();
        store.save("a", new Document());

        assertThrows(EditException.class, () -> store.get(null));
        assertFalse(store.has(null));
    }

    @Test
    void blankNamesAreRejected() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();

        assertThrows(EditException.class, () -> store.save("", d));
        assertThrows(EditException.class, () -> store.save("   ", d));
        assertThrows(EditException.class, () -> store.save(null, d));
        assertTrue(store.names().isEmpty());
    }

    //names() and has()

    @Test
    void namesStartEmpty() {
        CheckpointStore store = new CheckpointStore();

        assertTrue(store.names().isEmpty());
        assertFalse(store.has("anything"));
    }

    @Test
    void namesAreSorted() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();

        store.save("charlie", d);
        store.save("alpha", d);
        store.save("bravo", d);

        assertEquals(List.of("alpha", "bravo", "charlie"), store.names());
    }

    @Test
    void changingTheNamesListDoesNotChangeTheStore() {
        Document d = new Document();
        CheckpointStore store = new CheckpointStore();
        store.save("a", d);

        store.names().clear();

        assertEquals(List.of("a"), store.names());
    }
}
