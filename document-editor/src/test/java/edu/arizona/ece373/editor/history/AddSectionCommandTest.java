package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.Section;
import edu.arizona.ece373.editor.model.SectionPath;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AddSectionCommand} (6.2.4).
 *
 * <p>Covers the main case, invalid input rejected by validate with the document
 * unchanged, boundary positions (first subsection, after existing ones, a parent
 * that holds only paragraphs), and the undo round trip.
 */
class AddSectionCommandTest {

    //main case

    @Test
    void addsTopLevelSectionToAnEmptyDocument() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "Intro");

        c.validate(d);
        c.execute(d);

        assertEquals("1", c.addedPath().toString());
        assertEquals("Intro", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals(1, d.section(null).subsections().size());
    }

    @Test
    void addsTopLevelSectionAfterExistingOnes() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        AddSectionCommand c = new AddSectionCommand(null, "Three");

        c.execute(d);

        assertEquals("3", c.addedPath().toString());
        assertEquals("One", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals("Two", d.section(SectionPath.parse("2")).title().plainText());
        assertEquals("Three", d.section(SectionPath.parse("3")).title().plainText());
    }

    @Test
    void addsSubsectionsInOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        AddSectionCommand first = new AddSectionCommand(SectionPath.parse("1"), "Sub A");
        AddSectionCommand second = new AddSectionCommand(SectionPath.parse("1"), "Sub B");

        first.execute(d);
        second.execute(d);

        assertEquals("1.1", first.addedPath().toString());
        assertEquals("1.2", second.addedPath().toString());
        assertEquals("Sub A", d.section(SectionPath.parse("1.1")).title().plainText());
        assertEquals("Sub B", d.section(SectionPath.parse("1.2")).title().plainText());
    }

    @Test
    void addsAtAnyDepth() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        AddSectionCommand c = new AddSectionCommand(SectionPath.parse("1.1"), "Deep");

        c.validate(d);
        c.execute(d);

        assertEquals("1.1.1", c.addedPath().toString());
        assertEquals("Deep", d.section(SectionPath.parse("1.1.1")).title().plainText());
    }

    @Test
    void newSectionStartsEmpty() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "Intro");

        c.execute(d);

        Section s = d.section(c.addedPath());
        assertTrue(s.subsections().isEmpty());
        assertThrows(EditException.class, () -> d.paragraph(1));
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsAnEmptyTitle() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Section title cannot be empty", e.getMessage());
        assertEquals(0, d.section(null).subsections().size());
    }

    @Test
    void validateRejectsABlankTitle() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "   \t ");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals(0, d.section(null).subsections().size());
    }

    @Test
    void validateRejectsANullTitle() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, null);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals(0, d.section(null).subsections().size());
    }

    @Test
    void validateRejectsAMissingParent() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        AddSectionCommand c = new AddSectionCommand(SectionPath.parse("2"), "Orphan");

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertTrue(e.getMessage().contains("2"), e.getMessage());
        assertEquals(1, d.section(null).subsections().size());
        assertTrue(d.section(SectionPath.parse("1")).subsections().isEmpty());
        assertNull(c.addedPath());
    }

    @Test
    void validateRejectsAMissingNestedParent() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        AddSectionCommand c = new AddSectionCommand(SectionPath.parse("1.5"), "Orphan");

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals(1, d.section(SectionPath.parse("1")).subsections().size());
    }

    @Test
    void failedValidateFiresNoEvent() {
        Document d = new Document();
        int[] events = {0};
        d.addListener(e -> events[0]++);

        assertThrows(EditException.class,
                () -> new AddSectionCommand(null, " ").validate(d));
        assertThrows(EditException.class,
                () -> new AddSectionCommand(SectionPath.parse("3"), "Title").validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedAdd() {
        Document d = new Document();
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new AddSectionCommand(null, "")));

        assertFalse(h.canUndo());
        assertEquals(0, d.section(null).subsections().size());
    }

    //boundaries: first position (no subsections yet), position = length
    //(after existing subsections, covered above), and a parent that has
    //paragraphs but no subsections

    @Test
    void firstSubsectionOfAParentWithOnlyParagraphsIsNumberedOne() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "b").execute(d);
        AddSectionCommand c = new AddSectionCommand(SectionPath.parse("1"), "Sub");

        c.execute(d);

        // Paragraphs do not count toward section numbers.
        assertEquals("1.1", c.addedPath().toString());
        assertEquals("a", d.paragraph(1).text().plainText());
        assertEquals("b", d.paragraph(2).text().plainText());
    }

    @Test
    void singleCharacterTitleIsAccepted() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "A");

        c.validate(d);
        c.execute(d);

        assertEquals("A", d.section(SectionPath.parse("1")).title().plainText());
    }

    //undo round trip: execute then undo gives exactly the original

    @Test
    void undoRoundTripRestoresTheDocumentExactly() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "b").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "c").execute(d);
        // Same objects means same text AND same formatting: nothing was copied or rebuilt.
        Section s1 = d.section(SectionPath.parse("1"));
        Section s11 = d.section(SectionPath.parse("1.1"));
        Section s2 = d.section(SectionPath.parse("2"));
        var p1 = d.paragraph(1);
        var p2 = d.paragraph(2);
        var p3 = d.paragraph(3);
        AddSectionCommand c = new AddSectionCommand(SectionPath.parse("1"), "Temporary");

        c.execute(d);
        c.undo(d);

        assertSame(s1, d.section(SectionPath.parse("1")));
        assertSame(s11, d.section(SectionPath.parse("1.1")));
        assertSame(s2, d.section(SectionPath.parse("2")));
        assertThrows(EditException.class, () -> d.section(SectionPath.parse("1.2")));
        assertEquals(1, s1.subsections().size());
        assertEquals(2, d.section(null).subsections().size());
        assertSame(p1, d.paragraph(1));
        assertSame(p2, d.paragraph(2));
        assertSame(p3, d.paragraph(3));
        assertThrows(EditException.class, () -> d.paragraph(4));
    }

    @Test
    void undoTopLevelLeavesAnEmptyDocument() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "Only");
        c.execute(d);

        c.undo(d);

        assertEquals(0, d.section(null).subsections().size());
    }

    @Test
    void repeatedRoundTripsLeaveTheDocumentUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "Keep").execute(d);
        Section kept = d.section(SectionPath.parse("1"));
        AddSectionCommand c = new AddSectionCommand(null, "x");

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals(1, d.section(null).subsections().size());
        assertSame(kept, d.section(SectionPath.parse("1")));
    }

    @Test
    void redoAfterUndoPutsTheSameSectionBack() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "Again");
        c.execute(d);
        Section first = d.section(SectionPath.parse("1"));

        c.undo(d);
        c.execute(d);

        assertSame(first, d.section(SectionPath.parse("1")));
        assertEquals("1", c.addedPath().toString());
    }

    @Test
    void historyUndoesASectionAndTheParagraphAddedToIt() {
        Document d = new Document();
        History h = new History(d);

        h.perform(new AddSectionCommand(null, "Intro"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "hello"));
        h.undo();
        h.undo();

        assertEquals(0, d.section(null).subsections().size());

        h.redo();
        h.redo();
        assertEquals("Intro", d.section(SectionPath.parse("1")).title().plainText());
        assertEquals("hello", d.paragraph(1).text().plainText());
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        int[] events = {0};
        d.addListener(e -> events[0]++);
        AddSectionCommand c = new AddSectionCommand(null, "x");

        c.execute(d);
        assertEquals(1, events[0]);

        c.undo(d);
        assertEquals(2, events[0]);
    }

    @Test
    void describeChangesAfterExecute() {
        Document d = new Document();
        AddSectionCommand c = new AddSectionCommand(null, "Intro");

        assertNull(c.addedPath());
        assertEquals("add section \"Intro\"", c.describe());

        c.execute(d);

        assertEquals("added section 1 \"Intro\"", c.describe());
    }
}
