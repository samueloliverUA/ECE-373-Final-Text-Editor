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
 * Tests {@link InsertTextCommand} (6.3.1).
 *
 * <p>Covers the main case, invalid input rejected by validate with the document
 * unchanged, boundary positions (offset 0, offset = length, empty paragraph),
 * and the undo round trip.
 *
 * <p>Most tests start from the paragraph "Hello world" (11 characters):
 * offset 5 is just after "Hello" and offset 11 is the end.
 */
class InsertTextCommandTest {

    //main case

    @Test
    void insertsInTheMiddleOfAParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        c.validate(d);
        c.execute(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void onlyChangesTheNamedParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "first").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "second").execute(d);
        InsertTextCommand c = new InsertTextCommand(2, 0, "the ");

        c.validate(d);
        c.execute(d);

        assertEquals("first", d.paragraph(1).text().plainText());
        assertEquals("the second", d.paragraph(2).text().plainText());
    }

    @Test
    void worksInALaterSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "aaa").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "bbbddd").execute(d);
        InsertTextCommand c = new InsertTextCommand(2, 3, "ccc");

        c.validate(d);
        c.execute(d);

        assertEquals("aaa", d.paragraph(1).text().plainText());
        assertEquals("bbbcccddd", d.paragraph(2).text().plainText());
    }

    @Test
    void insertsASingleCharacter() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 5, ",");

        c.validate(d);
        c.execute(d);

        assertEquals("Hello, world", d.paragraph(1).text().plainText());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(2, 0, "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(0, 0, "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnyInsertInAnEmptyDocument() {
        Document d = new Document();
        InsertTextCommand c = new InsertTextCommand(1, 0, "x");

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsAnOffsetPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 12, "x");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 11 characters", e.getMessage());
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANegativeOffset() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, -1, "x");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsEmptyText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 3, "");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Nothing to insert", e.getMessage());
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsNullText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 3, null);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class,
                () -> new InsertTextCommand(1, 99, "x").validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedInsert() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new InsertTextCommand(1, 99, "x")));

        assertFalse(h.canUndo());
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    //boundaries: offset 0, offset = length, empty paragraph

    @Test
    void insertsAtOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 0, "Oh, ");

        c.validate(d);
        c.execute(d);

        assertEquals("Oh, Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void insertsAtOffsetEqualToTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 11, "!");

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world!", d.paragraph(1).text().plainText());
    }

    @Test
    void insertsIntoAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 0, "first words");

        c.validate(d);
        c.execute(d);

        assertEquals("first words", d.paragraph(1).text().plainText());
    }

    @Test
    void emptyParagraphRejectsAnyOffsetButZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 1, "x");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 0 characters", e.getMessage());
        assertEquals("", d.paragraph(1).text().plainText());
    }

    // undo round trip: execute then undo gives exactly the original

    @Test
    void undoRestoresTheOriginalText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        // Same objects means nothing was copied or rebuilt, so formatting is kept too.
        Paragraph p = d.paragraph(1);
        FormattedText t = p.text();
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        c.execute(d);
        c.undo(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertSame(p, d.paragraph(1));
        assertSame(t, d.paragraph(1).text());
    }

    @Test
    void undoRoundTripAtOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 0, "Oh, ");

        c.execute(d);
        c.undo(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripAtTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 11, "!");

        c.execute(d);
        c.undo(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripInAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 0, "temporary");

        c.execute(d);
        c.undo(d);

        assertEquals("", d.paragraph(1).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheTextUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void redoInsertsTheSameTextAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void historyUndoesTwoInsertsInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        History h = new History(d);

        h.perform(new InsertTextCommand(1, 11, "!"));      // "Hello world!"
        h.perform(new InsertTextCommand(1, 5, " brave"));  // "Hello brave world!"
        assertEquals("Hello brave world!", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("Hello world!", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("Hello world", d.paragraph(1).text().plainText());

        h.redo();
        h.redo();
        assertEquals("Hello brave world!", d.paragraph(1).text().plainText());
    }

    @Test
    void insertThenDeleteThroughHistoryUndoesCleanly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        History h = new History(d);

        h.perform(new InsertTextCommand(1, 5, " brave"));
        h.perform(new DeleteTextCommand(new TextRange(1, 0, 6)));
        assertEquals("brave world", d.paragraph(1).text().plainText());

        h.undo();
        h.undo();

        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeNamesTheLengthAndPosition() {
        InsertTextCommand c = new InsertTextCommand(1, 5, " brave");

        assertEquals("insert 6 characters at p1:5", c.describe());
    }
}
