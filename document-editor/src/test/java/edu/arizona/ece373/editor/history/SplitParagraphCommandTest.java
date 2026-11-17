package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.CharFormat;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;
import edu.arizona.ece373.editor.model.SectionPath;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SplitParagraphCommand} (6.3.5).
 *
 * <p>Covers the main case, invalid input rejected by validate with the document
 * unchanged, boundary positions (offset 0, offset = length, empty paragraph), and the
 * undo round trip, including formatting.
 *
 * <p>Most tests use "Hello world" (11 characters): offset 5 is just after "Hello".
 */
class SplitParagraphCommandTest {

    //main case

    @Test
    void splitsInTheMiddle() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals(" world", d.paragraph(2).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(3));
    }

    @Test
    void newParagraphGoesRightAfterTheOriginalInTheSameSection() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "z").execute(d);
        Section s = d.section(SectionPath.parse("1"));
        SplitParagraphCommand c = new SplitParagraphCommand(2, 5);

        c.validate(d);
        c.execute(d);

        assertEquals(4, s.children().size());
        assertEquals(2, s.indexOf(d.paragraph(3)));
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("Hello", d.paragraph(2).text().plainText());
        assertEquals(" world", d.paragraph(3).text().plainText());
        assertEquals("z", d.paragraph(4).text().plainText());
    }

    @Test
    void splitsInsideASubsection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "leftright").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 4);

        c.validate(d);
        c.execute(d);

        assertEquals("left", d.paragraph(1).text().plainText());
        assertEquals("right", d.paragraph(2).text().plainText());
        assertEquals(2, d.section(SectionPath.parse("1.1")).children().size());
    }

    @Test
    void splittingTheLastParagraphOfASectionStaysInThatSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "other").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        c.validate(d);
        c.execute(d);

        assertEquals(2, d.section(SectionPath.parse("1")).children().size());
        assertEquals(1, d.section(SectionPath.parse("2")).children().size());
        assertEquals(" world", d.paragraph(2).text().plainText());
        assertEquals("other", d.paragraph(3).text().plainText());
    }

    @Test
    void formattingIsKeptOnBothHalves() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        d.paragraph(1).text().applyFormat(3, 8, FormatChange.bold(true)); // "lo wo" bold
        CharFormat bold = d.paragraph(1).text().formatAt(3);
        CharFormat plain = d.paragraph(1).text().formatAt(0);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        c.execute(d);

        FormattedText left = d.paragraph(1).text();   // "Hel" plain, "lo" bold
        FormattedText right = d.paragraph(2).text();  // " wo" bold, "rld" plain
        assertEquals(plain, left.formatAt(2));
        assertEquals(bold, left.formatAt(3));
        assertEquals(bold, left.formatAt(4));
        assertEquals(bold, right.formatAt(0));
        assertEquals(bold, right.formatAt(2));
        assertEquals(plain, right.formatAt(3));
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAnOffsetPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 12);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 11 characters", e.getMessage());
        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void validateRejectsANegativeOffset() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, -1);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(2, 0);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(0, 0);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyDocument() {
        Document d = new Document();
        SplitParagraphCommand c = new SplitParagraphCommand(1, 0);

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class, () -> new SplitParagraphCommand(1, 99).validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedSplit() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        History h = new History(d);

        assertThrows(EditException.class, () -> h.perform(new SplitParagraphCommand(1, 99)));

        assertFalse(h.canUndo());
        assertEquals("Hello world", d.paragraph(1).text().plainText());
    }

    //boundaries: offset 0, offset = length, empty paragraph

    @Test
    void splitAtOffsetZeroLeavesAnEmptyFirstParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 0);

        c.validate(d);
        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
        assertEquals("Hello world", d.paragraph(2).text().plainText());
    }

    @Test
    void splitAtTheLengthMakesAnEmptyNewParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 11);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertEquals("", d.paragraph(2).text().plainText());
    }

    @Test
    void splittingAnEmptyParagraphMakesTwoEmptyParagraphs() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 0);

        c.validate(d);
        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
        assertEquals("", d.paragraph(2).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(3));
    }

    @Test
    void emptyParagraphRejectsAnyOffsetButZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 1);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 0 characters", e.getMessage());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void splitsASingleCharacterParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "ab").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 1);

        c.validate(d);
        c.execute(d);

        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoRestoresTextAndFormattingExactly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "z").execute(d);
        d.paragraph(2).text().applyFormat(3, 8, FormatChange.bold(true));
        Paragraph p1 = d.paragraph(1);
        Paragraph p2 = d.paragraph(2);
        Paragraph p3 = d.paragraph(3);
        FormattedText before = p2.text().copy();
        SplitParagraphCommand c = new SplitParagraphCommand(2, 5);

        c.execute(d);
        c.undo(d);

        assertSame(p1, d.paragraph(1));
        assertSame(p2, d.paragraph(2));
        assertSame(p3, d.paragraph(3));
        // FormattedText.equals compares the characters AND their formatting.
        assertEquals(before, d.paragraph(2).text());
        assertThrows(EditException.class, () -> d.paragraph(4));
        assertEquals(3, d.section(SectionPath.parse("1")).children().size());
    }

    @Test
    void undoRoundTripAtOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 0);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void undoRoundTripAtTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 11);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void undoRoundTripOfAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 0);

        c.execute(d);
        c.undo(d);

        assertEquals("", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void repeatedRoundTripsLeaveTheDocumentUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void redoSplitsAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals(" world", d.paragraph(2).text().plainText());
    }

    @Test
    void historyUndoesTwoSplitsInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "onetwothree").execute(d);
        History h = new History(d);

        h.perform(new SplitParagraphCommand(1, 6)); // "onetwo", "three"
        h.perform(new SplitParagraphCommand(1, 3)); // "one", "two", "three"
        assertEquals("one", d.paragraph(1).text().plainText());
        assertEquals("two", d.paragraph(2).text().plainText());
        assertEquals("three", d.paragraph(3).text().plainText());

        h.undo();
        assertEquals("onetwo", d.paragraph(1).text().plainText());
        assertEquals("three", d.paragraph(2).text().plainText());

        h.undo();
        assertEquals("onetwothree", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));

        h.redo();
        h.redo();
        assertEquals("one", d.paragraph(1).text().plainText());
        assertEquals("three", d.paragraph(3).text().plainText());
    }

    @Test
    void mergeUndoesASplit() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        d.paragraph(1).text().applyFormat(3, 8, FormatChange.bold(true));
        FormattedText before = d.paragraph(1).text().copy();

        new SplitParagraphCommand(1, 5).execute(d);
        new MergeParagraphsCommand(1).execute(d);

        assertEquals(before, d.paragraph(1).text());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void splitThenInsertThroughHistoryUndoesCleanly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        History h = new History(d);

        h.perform(new SplitParagraphCommand(1, 5));      // "Hello", " world"
        h.perform(new InsertTextCommand(2, 0, "big"));   // "Hello", "big world"
        assertEquals("big world", d.paragraph(2).text().plainText());

        h.undo();
        h.undo();

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        SplitParagraphCommand c = new SplitParagraphCommand(1, 5);

        assertEquals("split p1 at 5", c.describe());

        c.execute(d);

        assertEquals("split p1 at 5 into p1 and p2", c.describe());
    }
}
