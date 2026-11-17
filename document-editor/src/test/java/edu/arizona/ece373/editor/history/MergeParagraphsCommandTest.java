package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;
import edu.arizona.ece373.editor.model.SectionPath;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MergeParagraphsCommand} (6.3.6).
 *
 * <p>Covers the main case, invalid input rejected by validate with the document
 * unchanged (including the decision that both paragraphs must be in the same
 * section), boundary positions (empty first or second paragraph, so the boundary
 * is at offset 0 or at the end), and the undo round trip.
 */
class MergeParagraphsCommandTest {

    //main case

    @Test
    void appendsTheNextParagraphAndRemovesIt() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), " world").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
        assertEquals(5, c.boundary());
    }

    @Test
    void laterParagraphsMoveUpOneNumber() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "c").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "d").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(2);

        c.validate(d);
        c.execute(d);

        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("bc", d.paragraph(2).text().plainText());
        assertEquals("d", d.paragraph(3).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(4));
    }

    @Test
    void mergesInsideASubsection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "left").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "right").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("leftright", d.paragraph(1).text().plainText());
        assertEquals(1, d.section(SectionPath.parse("1.1")).children().size());
    }

    @Test
    void doesNotTouchOtherSections() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "c").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("ab", d.paragraph(1).text().plainText());
        assertEquals("c", d.paragraph(2).text().plainText());
        assertEquals(1, d.section(SectionPath.parse("2")).children().size());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsTheLastParagraphInTheDocument() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(2);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p2 is the last paragraph; nothing to merge", e.getMessage());
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
    }

    @Test
    void validateRejectsTheOnlyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "alone").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("alone", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(5);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(0);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("a", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyDocument() {
        Document d = new Document();
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsANextParagraphInTheNextSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "end of one").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "start of two").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 and p2 are in different sections", e.getMessage());
        assertEquals("end of one", d.paragraph(1).text().plainText());
        assertEquals("start of two", d.paragraph(2).text().plainText());
    }

    @Test
    void validateRejectsANextParagraphInASubsection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "parent").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "child").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("parent", d.paragraph(1).text().plainText());
        assertEquals("child", d.paragraph(2).text().plainText());
    }

    @Test
    void validateRejectsANextParagraphBackInTheParent() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "child").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "parent").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("child", d.paragraph(1).text().plainText());
        assertEquals("parent", d.paragraph(2).text().plainText());
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "alone").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class, () -> new MergeParagraphsCommand(1).validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedMerge() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "alone").execute(d);
        History h = new History(d);

        assertThrows(EditException.class, () -> h.perform(new MergeParagraphsCommand(1)));

        assertFalse(h.canUndo());
        assertEquals("alone", d.paragraph(1).text().plainText());
    }

    //boundaries: boundary at offset 0, at the end, and empty paragraphs

    @Test
    void emptyFirstParagraphGivesBoundaryZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "world").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("world", d.paragraph(1).text().plainText());
        assertEquals(0, c.boundary());
    }

    @Test
    void emptySecondParagraphGivesBoundaryAtTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals(5, c.boundary());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void twoEmptyParagraphsMergeIntoOneEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.validate(d);
        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void mergesTheLastTwoParagraphsOfASection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "c").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "d").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(2);

        c.validate(d);
        c.execute(d);

        assertEquals("bc", d.paragraph(2).text().plainText());
        assertEquals("d", d.paragraph(3).text().plainText());
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoRestoresBothParagraphsExactly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), " world").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "z").execute(d);
        // Same objects means nothing was copied or rebuilt, so formatting is kept too.
        Paragraph p1 = d.paragraph(1);
        Paragraph p2 = d.paragraph(2);
        Paragraph p3 = d.paragraph(3);
        Paragraph p4 = d.paragraph(4);
        MergeParagraphsCommand c = new MergeParagraphsCommand(2);

        c.execute(d);
        c.undo(d);

        assertSame(p1, d.paragraph(1));
        assertSame(p2, d.paragraph(2));
        assertSame(p3, d.paragraph(3));
        assertSame(p4, d.paragraph(4));
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("Hello", d.paragraph(2).text().plainText());
        assertEquals(" world", d.paragraph(3).text().plainText());
        assertEquals("z", d.paragraph(4).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(5));
    }

    @Test
    void undoPutsTheSecondParagraphBackAtItsOldIndex() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "c").execute(d);
        Section s = d.section(SectionPath.parse("1"));
        Paragraph b = d.paragraph(2);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.execute(d);
        c.undo(d);

        assertEquals(1, s.indexOf(b));
        assertEquals(3, s.children().size());
    }

    @Test
    void undoRoundTripWithAnEmptyFirstParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "world").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.execute(d);
        c.undo(d);

        assertEquals("", d.paragraph(1).text().plainText());
        assertEquals("world", d.paragraph(2).text().plainText());
    }

    @Test
    void undoRoundTripWithAnEmptySecondParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals("", d.paragraph(2).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheDocumentUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), " world").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals(" world", d.paragraph(2).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(3));
    }

    @Test
    void redoMergesAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), " world").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void historyUndoesTwoMergesInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "c").execute(d);
        History h = new History(d);

        h.perform(new MergeParagraphsCommand(1)); // "ab", "c"
        h.perform(new MergeParagraphsCommand(1)); // "abc"
        assertEquals("abc", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("ab", d.paragraph(1).text().plainText());
        assertEquals("c", d.paragraph(2).text().plainText());

        h.undo();
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
        assertEquals("c", d.paragraph(3).text().plainText());

        h.redo();
        h.redo();
        assertEquals("abc", d.paragraph(1).text().plainText());
    }

    @Test
    void mergeThenInsertThroughHistoryUndoesCleanly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "world").execute(d);
        History h = new History(d);

        h.perform(new MergeParagraphsCommand(1));        // "Helloworld"
        h.perform(new InsertTextCommand(1, 5, " "));     // "Hello world"
        assertEquals("Hello world", d.paragraph(1).text().plainText());

        h.undo();
        h.undo();

        assertEquals("Hello", d.paragraph(1).text().plainText());
        assertEquals("world", d.paragraph(2).text().plainText());
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        MergeParagraphsCommand c = new MergeParagraphsCommand(1);

        assertEquals(-1, c.boundary());
        assertEquals("merge p1 with p2", c.describe());

        c.execute(d);

        assertEquals("merged p2 into p1", c.describe());
    }
}
