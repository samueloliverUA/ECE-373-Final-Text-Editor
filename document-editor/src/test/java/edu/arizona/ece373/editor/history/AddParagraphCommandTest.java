package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.SectionPath;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AddParagraphCommand} (6.2.4).
 *
 * <p>Covers the main case, invalid input rejected by validate with the document
 * unchanged, boundary positions (empty section, end of a section, empty
 * paragraph), and the undo round trip.
 */
class AddParagraphCommandTest {

    //main case

    @Test
    void addsParagraphToAnEmptySection() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "Hello world");

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world", d.paragraph(1).text().plainText());
        assertEquals(1, c.addedNumber());
    }

    @Test
    void addsToTheEndOfTheSection() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "first").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "second");

        c.execute(d);

        assertEquals("first", d.paragraph(1).text().plainText());
        assertEquals("second", d.paragraph(2).text().plainText());
        assertEquals(2, c.addedNumber());
    }

    @Test
    void numberCountsParagraphsAcrossSections() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "b").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "new");

        c.execute(d);

        // New paragraph goes at the end of section 1, so "b" moves to p3.
        assertEquals(2, c.addedNumber());
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("new", d.paragraph(2).text().plainText());
        assertEquals("b", d.paragraph(3).text().plainText());
    }

    @Test
    void addsToASubsection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One point one").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1.1"), "nested");

        c.validate(d);
        c.execute(d);

        assertEquals("nested", d.paragraph(1).text().plainText());
        assertSame(d.paragraph(1), d.paragraph(c.addedNumber()));
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoRoundTripRestoresTheDocumentExactly() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One point one").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "b").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "c").execute(d);
        // Same objects means same text AND same formatting: nothing was copied or rebuilt.
        var p1 = d.paragraph(1);
        var p2 = d.paragraph(2);
        var p3 = d.paragraph(3);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "temporary");

        c.execute(d);
        c.undo(d);

        assertSame(p1, d.paragraph(1));
        assertSame(p2, d.paragraph(2));
        assertSame(p3, d.paragraph(3));
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
        assertEquals("c", d.paragraph(3).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(4));
    }

    @Test
    void repeatedRoundTripsLeaveTheDocumentUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "keep").execute(d);
        var kept = d.paragraph(1);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "x");

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertSame(kept, d.paragraph(1));
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    //undo / redo

    @Test
    void undoRemovesTheParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "keep").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "remove me");
        c.execute(d);

        c.undo(d);

        assertEquals("keep", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
    }

    @Test
    void undoTheOnlyParagraphLeavesTheSectionEmpty() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "only");
        c.execute(d);

        c.undo(d);

        assertThrows(EditException.class, () -> d.paragraph(1));
    }

    @Test
    void redoAfterUndoPutsTheSameParagraphBack() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "again");
        c.execute(d);
        var first = d.paragraph(1);

        c.undo(d);
        c.execute(d);

        assertEquals("again", d.paragraph(1).text().plainText());
        assertSame(first, d.paragraph(1));
    }

    @Test
    void worksThroughHistory() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        History h = new History(d);

        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "one"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "two"));
        h.undo();

        assertEquals("one", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));

        h.redo();
        assertEquals("two", d.paragraph(2).text().plainText());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAMissingTopLevelSection() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "existing").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("2"), "nope");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertTrue(e.getMessage().contains("2"), e.getMessage());
        assertEquals("existing", d.paragraph(1).text().plainText());
        assertThrows(EditException.class, () -> d.paragraph(2));
        assertEquals(-1, c.addedNumber());
    }

    @Test
    void validateRejectsAMissingSubsection() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1.3"), "nope");

        assertThrows(EditException.class, () -> c.validate(d));
        assertThrows(EditException.class, () -> d.paragraph(1));
    }

    @Test
    void validateRejectsAnyPathInAnEmptyDocument() {
        Document d = new Document();
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "nope");

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("5"), "nope");

        assertThrows(EditException.class, () -> c.validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedAdd() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new AddParagraphCommand(SectionPath.parse("9"), "nope")));

        assertFalse(h.canUndo());
    }

    //boundaries: position 0 (empty section) and position = length (end of
    //section) are covered by addsParagraphToAnEmptySection and
    //addsToTheEndOfTheSection above; empty paragraph below

    @Test
    void emptyTextMakesAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "");

        c.execute(d);

        assertEquals(0, d.paragraph(1).text().length());
    }

    @Test
    void nullTextIsTreatedAsEmpty() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), null);

        c.execute(d);

        assertEquals("", d.paragraph(1).text().plainText());
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "x");

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        new AddSectionCommand(null, "Intro").execute(d);
        AddParagraphCommand c = new AddParagraphCommand(SectionPath.parse("1"), "x");

        assertEquals(-1, c.addedNumber());
        assertEquals("add paragraph to section 1", c.describe());

        c.execute(d);

        assertEquals("added paragraph 1 to section 1", c.describe());
    }
}
