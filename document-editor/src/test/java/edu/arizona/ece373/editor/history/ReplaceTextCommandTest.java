package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ReplaceTextCommand} (6.3.3).
 *
 * <p>Covers the main case (longer, shorter, same-length and empty replacements),
 * invalid ranges rejected by validate with the document unchanged, boundary
 * positions (offset 0, offset = length, whole paragraph, empty paragraph), and the
 * undo round trip.
 *
 * <p>Most tests use "the cat and the dog" (19 characters): "the" is 0-3, "cat" is
 * 4-7, "and" is 8-11, the second "the" is 12-15 and "dog" is 16-19.
 */
class ReplaceTextCommandTest {

    // main case

    @Test
    void replacesWithLongerText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        c.validate(d);
        c.execute(d);

        assertEquals("the lion and the dog", d.paragraph(1).text().plainText());
        assertEquals("cat", c.removedText().plainText());
    }

    @Test
    void replacesWithShorterText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 8, 11), "&");

        c.validate(d);
        c.execute(d);

        assertEquals("the cat & the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void replacesWithTextOfTheSameLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "cow");

        c.validate(d);
        c.execute(d);

        assertEquals("the cow and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void emptyReplacementDeletesTheRange() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 3, 7), "");

        c.validate(d);
        c.execute(d);

        assertEquals("the and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void nullReplacementIsTreatedAsEmpty() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 3, 7), null);

        c.validate(d);
        c.execute(d);

        assertEquals("the and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void onlyChangesTheNamedParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(2, 4, 7), "dog");

        c.validate(d);
        c.execute(d);

        assertEquals("the cat", d.paragraph(1).text().plainText());
        assertEquals("the dog", d.paragraph(2).text().plainText());
    }

    @Test
    void replaceAllWorksAsACompositeAddedLastToFirst() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        History h = new History(d);
        CompositeCommand all = new CompositeCommand("replace all");
        // Last match first, so the earlier range is still correct when it runs.
        all.add(new ReplaceTextCommand(new TextRange(1, 12, 15), "a"));
        all.add(new ReplaceTextCommand(new TextRange(1, 0, 3), "a"));

        h.perform(all);
        assertEquals("a cat and a dog", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    // ---- invalid input: rejected by validate, document unchanged ----

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(2, 0, 1), "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
        assertNull(c.removedText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(0, 0, 1), "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyDocument() {
        Document d = new Document();
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 1), "x");

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsAnEndPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 16, 20), "x");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 19 characters", e.getMessage());
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAStartAfterTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 7, 4), "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANegativeStart() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, -1, 3), "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyRange() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 4), "x");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Nothing to replace: p1:4-4 is empty", e.getMessage());
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class,
                () -> new ReplaceTextCommand(new TextRange(1, 0, 99), "x").validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedReplace() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new ReplaceTextCommand(new TextRange(1, 0, 99), "x")));

        assertFalse(h.canUndo());
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    // ---- boundaries: offset 0, offset = length, whole paragraph, empty paragraph ----

    @Test
    void replacesFromOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 3), "A");

        c.validate(d);
        c.execute(d);

        assertEquals("A cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void replacesUpToTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 16, 19), "bird");

        c.validate(d);
        c.execute(d);

        assertEquals("the cat and the bird", d.paragraph(1).text().plainText());
    }

    @Test
    void replacesTheWholeParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 19), "new text");

        c.validate(d);
        c.execute(d);

        assertEquals("new text", d.paragraph(1).text().plainText());
    }

    @Test
    void replacingTheWholeParagraphWithNothingLeavesItEmpty() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 19), "");

        c.validate(d);
        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
    }

    @Test
    void replacesASingleCharacter() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 5), "b");

        c.validate(d);
        c.execute(d);

        assertEquals("the bat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void nothingCanBeReplacedInAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);

        assertThrows(EditException.class,
                () -> new ReplaceTextCommand(new TextRange(1, 0, 0), "x").validate(d));
        assertThrows(EditException.class,
                () -> new ReplaceTextCommand(new TextRange(1, 0, 1), "x").validate(d));
        assertEquals("", d.paragraph(1).text().plainText());
    }

    // ---- undo round trip: execute then undo gives exactly the original ----

    @Test
    void undoRestoresTheOriginalText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        // Same objects means nothing was copied or rebuilt, so formatting is kept too.
        Paragraph p = d.paragraph(1);
        FormattedText t = p.text();
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
        assertSame(p, d.paragraph(1));
        assertSame(t, d.paragraph(1).text());
    }

    @Test
    void undoRoundTripWithShorterText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 8, 11), "&");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripWithAnEmptyReplacement() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 3, 7), "");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripAtOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 3), "A");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripAtTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 16, 19), "bird");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripOfTheWholeParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 0, 19), "new text");

        c.execute(d);
        c.undo(d);

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheTextUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void redoReplacesAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("the lion and the dog", d.paragraph(1).text().plainText());
    }

    @Test
    void historyUndoesTwoReplacesInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        History h = new History(d);

        h.perform(new ReplaceTextCommand(new TextRange(1, 4, 7), "lion"));   // "the lion and the dog"
        h.perform(new ReplaceTextCommand(new TextRange(1, 17, 20), "wolf")); // "the lion and the wolf"
        assertEquals("the lion and the wolf", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("the lion and the dog", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());

        h.redo();
        h.redo();
        assertEquals("the lion and the wolf", d.paragraph(1).text().plainText());
    }

    @Test
    void replaceThenInsertThroughHistoryUndoesCleanly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        History h = new History(d);

        h.perform(new ReplaceTextCommand(new TextRange(1, 4, 7), "lion")); // "the lion and the dog"
        h.perform(new InsertTextCommand(1, 4, "big "));                    // "the big lion and the dog"
        assertEquals("the big lion and the dog", d.paragraph(1).text().plainText());

        h.undo();
        h.undo();

        assertEquals("the cat and the dog", d.paragraph(1).text().plainText());
    }

    // ---- events and describe ----

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        ReplaceTextCommand c = new ReplaceTextCommand(new TextRange(1, 4, 7), "lion");

        assertEquals("replace p1:4-7 with \"lion\"", c.describe());

        c.execute(d);

        assertEquals("replaced \"cat\" with \"lion\" at p1:4-7", c.describe());
    }
}
