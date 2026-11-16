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
 * Tests {@link DeleteTextCommand} (6.3.2).
 *
 * <p>Covers the main case, invalid ranges rejected by validate with the document
 * unchanged, boundary positions (offset 0, offset = length, whole paragraph,
 * empty paragraph), and the undo round trip.
 *
 * <p>Most tests use the paragraph "Hello brave world" (17 characters):
 * "Hello" is 0-5, " brave" is 5-11 and " world" is 11-17.
 */
class DeleteTextCommandTest {

    //main case

    @Test
    void deletesTheMiddleOfAParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertEquals(" brave", c.removedText().plainText());
    }

    @Test
    void onlyChangesTheNamedParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "first paragraph").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "second paragraph").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(2, 0, 7));

        c.validate(d);
        c.execute(d);

        assertEquals("first paragraph", d.paragraph(1).text().plainText());
        assertEquals("paragraph", d.paragraph(2).text().plainText());
    }

    @Test
    void worksInALaterSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "aaa").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "bbbccc").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(2, 3, 6));

        c.validate(d);
        c.execute(d);

        assertEquals("aaa", d.paragraph(1).text().plainText());
        assertEquals("bbb", d.paragraph(2).text().plainText());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(2, 0, 1));

        assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
        assertNull(c.removedText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(0, 0, 1));

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnyRangeInAnEmptyDocument() {
        Document d = new Document();
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 0, 1));

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsAnEndPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 18));

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 17 characters", e.getMessage());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAStartAfterTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 11, 5));

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANegativeStart() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, -1, 3));

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyRange() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 5));

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class,
                () -> new DeleteTextCommand(new TextRange(1, 0, 99)).validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedDelete() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new DeleteTextCommand(new TextRange(1, 0, 99))));

        assertFalse(h.canUndo());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    //boundaries: offset 0, offset = length, whole paragraph, empty paragraph

    @Test
    void deletesFromOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 0, 6));

        c.validate(d);
        c.execute(d);

        assertEquals("brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void deletesUpToTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 11, 17));

        c.validate(d);
        c.execute(d);

        assertEquals("Hello brave", d.paragraph(1).text().plainText());
    }

    @Test
    void deletesOneCharacterAtEachEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand first = new DeleteTextCommand(new TextRange(1, 0, 1));
        DeleteTextCommand last = new DeleteTextCommand(new TextRange(1, 15, 16));

        first.validate(d);
        first.execute(d);
        last.validate(d);
        last.execute(d);

        assertEquals("ello brave worl", d.paragraph(1).text().plainText());
    }

    @Test
    void deletingTheWholeTextLeavesAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 0, 17));

        c.validate(d);
        c.execute(d);

        // The paragraph is still there, just empty.
        assertEquals("", d.paragraph(1).text().plainText());
        assertEquals(0, d.paragraph(1).text().length());
    }

    @Test
    void nothingCanBeDeletedFromAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);

        assertThrows(EditException.class,
                () -> new DeleteTextCommand(new TextRange(1, 0, 0)).validate(d));
        assertThrows(EditException.class,
                () -> new DeleteTextCommand(new TextRange(1, 0, 1)).validate(d));
        assertEquals("", d.paragraph(1).text().plainText());
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoRestoresTheOriginalText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        // Same objects means nothing was copied or rebuilt, so formatting is kept too.
        Paragraph p = d.paragraph(1);
        FormattedText t = p.text();
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
        assertSame(p, d.paragraph(1));
        assertSame(t, d.paragraph(1).text());
    }

    @Test
    void undoRoundTripAtOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 0, 6));

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripAtTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 11, 17));

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripOfTheWholeText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 0, 17));

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheTextUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void redoDeletesTheSameRangeAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void historyUndoesTwoDeletesInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        History h = new History(d);

        h.perform(new DeleteTextCommand(new TextRange(1, 11, 17))); // "Hello brave"
        h.perform(new DeleteTextCommand(new TextRange(1, 0, 6)));   // "brave"
        assertEquals("brave", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("Hello brave", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());

        h.redo();
        h.redo();
        assertEquals("brave", d.paragraph(1).text().plainText());
    }

    // ---- events and describe ----

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        DeleteTextCommand c = new DeleteTextCommand(new TextRange(1, 5, 11));

        assertNull(c.removedText());
        assertEquals("delete p1:5-11", c.describe());

        c.execute(d);

        assertEquals("deleted 6 characters from p1:5-11", c.describe());
    }
}