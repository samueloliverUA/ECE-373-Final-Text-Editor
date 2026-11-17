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
 * Tests {@link MoveTextCommand} (6.3.4).
 *
 * <p>Covers the main case (moving forward and backward, with positions measured
 * before the move), invalid input rejected by validate with the document unchanged,
 * boundary positions (offset 0, offset = length, empty paragraph), and the undo
 * round trip.
 *
 * <p>Most tests use "Hello brave world" (17 characters): "Hello" is 0-5,
 * " brave" is 5-11 and " world" is 11-17.
 */
class MoveTextCommandTest {

    //main case

    @Test
    void movesTextForward() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 17);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world brave", d.paragraph(1).text().plainText());
    }

    @Test
    void movesTextBackward() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 11, 17), 1, 5);

        c.validate(d);
        c.execute(d);

        assertEquals("Hello world brave", d.paragraph(1).text().plainText());
    }

    @Test
    void destinationAfterTheSourceIsMeasuredBeforeTheMove() {
        // "one two three": "one" is 0-3, offset 7 is just before " three".
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two three").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 3), 1, 7);

        c.validate(d);
        c.execute(d);

        // "one" lands where " three" started in the ORIGINAL text.
        assertEquals(" twoone three", d.paragraph(1).text().plainText());
    }

    @Test
    void destinationBeforeTheSourceIsMeasuredBeforeTheMove() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two three").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 7, 13), 1, 3);

        c.validate(d);
        c.execute(d);

        assertEquals("one three two", d.paragraph(1).text().plainText());
    }

    @Test
    void movesASingleCharacter() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "abc").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 1), 1, 3);

        c.validate(d);
        c.execute(d);

        assertEquals("bca", d.paragraph(1).text().plainText());
    }

    @Test
    void onlyChangesTheNamedParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "first").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(2, 5, 11), 2, 17);

        c.validate(d);
        c.execute(d);

        assertEquals("first", d.paragraph(1).text().plainText());
        assertEquals("Hello world brave", d.paragraph(2).text().plainText());
    }

    //invalid input: rejected by validate, document unchanged

    @Test
    void validateRejectsADifferentDestinationParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "other").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 2, 0);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Text can only be moved within one paragraph", e.getMessage());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
        assertEquals("other", d.paragraph(2).text().plainText());
    }

    @Test
    void validateRejectsAMissingParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(2, 0, 1), 2, 3);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsParagraphZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(0, 0, 1), 0, 3);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyDocument() {
        Document d = new Document();
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 1), 1, 2);

        assertThrows(EditException.class, () -> c.validate(d));
    }

    @Test
    void validateRejectsADestinationInsideTheSource() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 8);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Destination p1:8 must be outside p1:5-11", e.getMessage());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsADestinationAtTheSourceStart() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 5);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsADestinationAtTheSourceEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 11);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsAnEmptyRange() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 5), 1, 0);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("Nothing to move: p1:5-5 is empty", e.getMessage());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsARangeEndPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 18), 1, 0);

        EditException e = assertThrows(EditException.class, () -> c.validate(d));

        assertEquals("p1 has 17 characters", e.getMessage());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsARangeStartAfterItsEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 11, 5), 1, 0);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANegativeRangeStart() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, -1, 3), 1, 9);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsADestinationPastTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 18);

        assertThrows(EditException.class, () -> c.validate(d));
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void validateRejectsANegativeDestination() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, -1);

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
                () -> new MoveTextCommand(new TextRange(1, 5, 11), 1, 8).validate(d));

        assertEquals(0, events[0]);
    }

    @Test
    void historyDoesNotRecordARejectedMove() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        History h = new History(d);

        assertThrows(EditException.class,
                () -> h.perform(new MoveTextCommand(new TextRange(1, 5, 11), 1, 8)));

        assertFalse(h.canUndo());
        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    //boundaries: offset 0, offset = length, empty paragraph

    @Test
    void movesToOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 0);

        c.validate(d);
        c.execute(d);

        assertEquals(" braveHello world", d.paragraph(1).text().plainText());
    }

    @Test
    void movesToOffsetEqualToTheLength() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 5), 1, 17);

        c.validate(d);
        c.execute(d);

        assertEquals(" brave worldHello", d.paragraph(1).text().plainText());
    }

    @Test
    void movesARangeThatEndsAtTheLengthToOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 11, 17), 1, 0);

        c.validate(d);
        c.execute(d);

        assertEquals(" worldHello brave", d.paragraph(1).text().plainText());
    }

    @Test
    void destinationsJustOutsideTheSourceAreAllowed() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);

        new MoveTextCommand(new TextRange(1, 5, 11), 1, 4).validate(d);
        new MoveTextCommand(new TextRange(1, 5, 11), 1, 12).validate(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void theWholeParagraphCannotBeMoved() {
        // Every destination is the range's start, end, or inside it.
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);

        assertThrows(EditException.class,
                () -> new MoveTextCommand(new TextRange(1, 0, 17), 1, 0).validate(d));
        assertThrows(EditException.class,
                () -> new MoveTextCommand(new TextRange(1, 0, 17), 1, 17).validate(d));
    }

    @Test
    void nothingCanBeMovedInAnEmptyParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);

        assertThrows(EditException.class,
                () -> new MoveTextCommand(new TextRange(1, 0, 0), 1, 0).validate(d));
        assertThrows(EditException.class,
                () -> new MoveTextCommand(new TextRange(1, 0, 1), 1, 0).validate(d));
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
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 17);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
        assertSame(p, d.paragraph(1));
        assertSame(t, d.paragraph(1).text());
    }

    @Test
    void undoRoundTripMovingBackward() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 11, 17), 1, 5);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripToOffsetZero() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 0);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void undoRoundTripToTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 5), 1, 17);

        c.execute(d);
        c.undo(d);

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    @Test
    void repeatedRoundTripsLeaveTheTextUnchanged() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two three").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 0, 3), 1, 7);

        for (int i = 0; i < 3; i++) {
            c.execute(d);
            c.undo(d);
        }

        assertEquals("one two three", d.paragraph(1).text().plainText());
    }

    @Test
    void redoMovesTheTextAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 17);

        c.execute(d);
        c.undo(d);
        c.execute(d);

        assertEquals("Hello world brave", d.paragraph(1).text().plainText());
    }

    @Test
    void historyUndoesTwoMovesInReverseOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two three").execute(d);
        History h = new History(d);

        h.perform(new MoveTextCommand(new TextRange(1, 7, 13), 1, 3)); // "one three two"
        h.perform(new MoveTextCommand(new TextRange(1, 0, 3), 1, 13)); // " three twoone"
        assertEquals(" three twoone", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("one three two", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("one two three", d.paragraph(1).text().plainText());

        h.redo();
        h.redo();
        assertEquals(" three twoone", d.paragraph(1).text().plainText());
    }

    @Test
    void moveThenDeleteThroughHistoryUndoesCleanly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        History h = new History(d);

        h.perform(new MoveTextCommand(new TextRange(1, 5, 11), 1, 17)); // "Hello world brave"
        h.perform(new DeleteTextCommand(new TextRange(1, 0, 6)));        // "world brave"
        assertEquals("world brave", d.paragraph(1).text().plainText());

        h.undo();
        h.undo();

        assertEquals("Hello brave world", d.paragraph(1).text().plainText());
    }

    //events and describe

    @Test
    void executeAndUndoEachFireOneEvent() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello brave world").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 17);

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
        MoveTextCommand c = new MoveTextCommand(new TextRange(1, 5, 11), 1, 17);

        assertEquals("move p1:5-11 to p1:17", c.describe());

        c.execute(d);

        assertEquals("moved 6 characters from p1:5-11 to p1:17", c.describe());
    }
}
