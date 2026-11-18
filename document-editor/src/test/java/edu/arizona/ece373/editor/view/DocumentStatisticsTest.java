package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.CheckpointStore;
import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.DeleteTextCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.history.InsertTextCommand;
import edu.arizona.ece373.editor.history.MergeParagraphsCommand;
import edu.arizona.ece373.editor.history.MoveTextCommand;
import edu.arizona.ece373.editor.history.ReplaceTextCommand;
import edu.arizona.ece373.editor.history.RestoreCheckpointCommand;
import edu.arizona.ece373.editor.history.SplitParagraphCommand;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DocumentStatistics} and {@link Statistics} (6.7.1-6.7.3).
 *
 * <p>Covers counts after edits, undo, redo, a checkpoint restore and a macro; that
 * documentChanged only sets the stale flag; the empty document giving 0, 0, 0; and the
 * counting rules (whitespace-separated words, spaces counted, titles not counted).
 *
 * <p>Each test registers the statistics object as a listener, the way the Editor does.
 */
class DocumentStatisticsTest {

    //empty document gives 0, 0, 0

    @Test
    void emptyDocumentGivesZeros() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);

        assertEquals(new Statistics(0, 0, 0), stats.current());
    }

    @Test
    void sectionsWithNoParagraphsGiveZeros() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "A title with four words").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "Sub title").execute(d);

        assertEquals(new Statistics(0, 0, 0), stats.current());
    }

    @Test
    void anEmptyParagraphCountsAsAParagraphOnly() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);

        assertEquals(new Statistics(0, 0, 1), stats.current());
    }

    //counting rules

    @Test
    void countsWordsCharactersAndParagraphs() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);

        Statistics s = stats.current();

        assertEquals(2, s.words());
        assertEquals(11, s.characters()); // the space counts
        assertEquals(1, s.paragraphs());
    }

    @Test
    void extraWhitespaceDoesNotMakeExtraWords() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "  a   b\tc  ").execute(d);

        Statistics s = stats.current();

        assertEquals(3, s.words());
        assertEquals(11, s.characters());
    }

    @Test
    void onlySpacesIsZeroWords() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "   ").execute(d);

        assertEquals(new Statistics(0, 3, 1), stats.current());
    }

    @Test
    void punctuationStaysPartOfAWord() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "don't stop - ever.").execute(d);

        assertEquals(4, stats.current().words()); // "don't", "stop", "-", "ever."
    }

    @Test
    void countsEveryParagraphInEverySectionButNotTitles() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "Long section title here").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "Sub").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "three").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "four five six").execute(d);

        // Words: 2 + 1 + 3; characters: 7 + 5 + 13; titles ignored.
        assertEquals(new Statistics(6, 25, 3), stats.current());
    }

    //counts correct after edits

    @Test
    void countsFollowEachKindOfEdit() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        assertEquals(new Statistics(2, 11, 1), stats.current());

        new InsertTextCommand(1, 5, " brave").execute(d);          // "Hello brave world"
        assertEquals(new Statistics(3, 17, 1), stats.current());

        new SplitParagraphCommand(1, 11).execute(d);               // "Hello brave", " world"
        assertEquals(new Statistics(3, 17, 2), stats.current());

        new DeleteTextCommand(new TextRange(1, 5, 11)).execute(d); // "Hello", " world"
        assertEquals(new Statistics(2, 11, 2), stats.current());

        new MergeParagraphsCommand(1).execute(d);                  // "Hello world"
        assertEquals(new Statistics(2, 11, 1), stats.current());

        new ReplaceTextCommand(new TextRange(1, 6, 11), "there, friend").execute(d);
        assertEquals(new Statistics(3, 19, 1), stats.current());  // "Hello there, friend"
    }

    @Test
    void movingTextChangesNoCounts() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one two three").execute(d);
        Statistics before = stats.current();

        new MoveTextCommand(new TextRange(1, 7, 13), 1, 3).execute(d); // "one three two"

        assertTrue(stats.isStale());
        assertEquals(before, stats.current());
    }

    @Test
    void formattingChangesNoCounts() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Hello world").execute(d);
        Statistics before = stats.current();

        d.paragraph(1).text().applyFormat(0, 5, FormatChange.bold(true));
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));

        assertEquals(before, stats.current());
    }

    //counts correct after undo and redo

    @Test
    void undoAndRedoUpdateTheCounts() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        History h = new History(d);
        h.perform(new AddSectionCommand(null, "S"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "one"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "two words"));
        assertEquals(new Statistics(3, 12, 2), stats.current());

        h.undo();
        assertEquals(new Statistics(1, 3, 1), stats.current());

        h.undo();
        assertEquals(new Statistics(0, 0, 0), stats.current());

        h.redo();
        h.redo();
        assertEquals(new Statistics(3, 12, 2), stats.current());
    }

    //counts correct after a checkpoint restor

    @Test
    void restoreAndItsUndoUpdateTheCounts() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        History h = new History(d);
        CheckpointStore store = new CheckpointStore();
        h.perform(new AddSectionCommand(null, "S"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "short"));
        store.save("small", d);
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "a much longer second paragraph"));
        assertEquals(new Statistics(6, 35, 2), stats.current());

        h.perform(new RestoreCheckpointCommand(store, "small"));
        assertEquals(new Statistics(1, 5, 1), stats.current());

        h.undo();
        assertEquals(new Statistics(6, 35, 2), stats.current());
    }

    @Test
    void editsAfterARestoreAreCounted() {
        // After a restore the document has a new root; the counts must follow it.
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        CheckpointStore store = new CheckpointStore();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one").execute(d);
        store.save("a", d);
        new RestoreCheckpointCommand(store, "a").execute(d);

        new InsertTextCommand(1, 3, " two").execute(d);

        assertEquals(new Statistics(2, 7, 1), stats.current());
    }

    //counts correct after a macro (replayed as one undoable entry

    @Test
    void macroUpdatesTheCountsAndUndoesAsOne() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        History h = new History(d);
        h.perform(new AddSectionCommand(null, "S"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "start"));
        Statistics before = stats.current();
        // A macro replays its steps as one CompositeCommand (one history entry).
        CompositeCommand macro = new CompositeCommand("macro: add two lines");
        macro.add(new AddParagraphCommand(SectionPath.parse("1"), "line one"));
        macro.add(new AddParagraphCommand(SectionPath.parse("1"), "line two"));

        h.perform(macro);
        assertEquals(new Statistics(5, 21, 3), stats.current());

        h.undo();
        assertEquals(before, stats.current());
    }

    //documentChanged does not recount (checked via the stale flag)

    @Test
    void startsStale() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);

        assertTrue(stats.isStale());
    }

    @Test
    void currentClearsTheStaleFlag() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);

        stats.current();

        assertFalse(stats.isStale());
    }

    @Test
    void documentChangedOnlySetsTheStaleFlag() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one").execute(d);
        assertEquals(new Statistics(1, 3, 1), stats.current());

        // Change the text WITHOUT firing an event: the counts must not move,
        // because nothing told the statistics to recount.
        d.paragraph(1).text().insert(3, " two");
        assertFalse(stats.isStale());
        assertEquals(new Statistics(1, 3, 1), stats.current());

        // Now fire the event: it only marks stale...
        stats.documentChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
        assertTrue(stats.isStale());

        // ...and the recount happens in current().
        assertEquals(new Statistics(2, 7, 1), stats.current());
        assertFalse(stats.isStale());
    }

    @Test
    void manyEditsThenOneCurrentRecountsOnce() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "x").execute(d);
        stats.current();

        for (int i = 0; i < 100; i++) {
            new InsertTextCommand(1, 0, "a ").execute(d);
        }

        assertTrue(stats.isStale());
        assertEquals(new Statistics(101, 201, 1), stats.current());
        assertFalse(stats.isStale());
    }

    @Test
    void currentTwiceWithNoChangeStaysFresh() {
        Document d = new Document();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "one").execute(d);

        Statistics first = stats.current();
        Statistics second = stats.current();

        assertFalse(stats.isStale());
        assertEquals(first, second);
    }

    //Statistics

    @Test
    void statisticsHoldsItsThreeCounts() {
        Statistics s = new Statistics(12, 60, 3);

        assertEquals(12, s.words());
        assertEquals(60, s.characters());
        assertEquals(3, s.paragraphs());
    }

    @Test
    void statisticsEqualityComparesAllThreeCounts() {
        assertEquals(new Statistics(1, 2, 3), new Statistics(1, 2, 3));
        assertEquals(new Statistics(1, 2, 3).hashCode(), new Statistics(1, 2, 3).hashCode());
        assertNotEquals(new Statistics(1, 2, 3), new Statistics(9, 2, 3));
        assertNotEquals(new Statistics(1, 2, 3), new Statistics(1, 9, 3));
        assertNotEquals(new Statistics(1, 2, 3), new Statistics(1, 2, 9));
    }

    @Test
    void statisticsToStringIsTheStatsOutput() {
        assertEquals("Words: 2, characters: 11, paragraphs: 1",
                new Statistics(2, 11, 1).toString());
    }
}
