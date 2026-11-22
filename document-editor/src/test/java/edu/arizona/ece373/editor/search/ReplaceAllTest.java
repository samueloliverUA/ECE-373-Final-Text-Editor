package edu.arizona.ece373.editor.search;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.model.CharFormat;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.SectionPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests Replace All (6.8.3, 6.8.4): {@link SearchService#findAll} followed by
 * {@link SearchService#replaceAllCommand}, run through {@link History} the way the
 * Editor does it.
 *
 * <p>Covers several matches in one paragraph with replacements longer, shorter and the
 * same length as the query, and that one undo restores the original text exactly,
 * formatting included.
 */
class ReplaceAllTest {

    //several matches in one paragraph

    @Test
    void longerReplacementInOneParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a cat, a cat and a cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "tiger"));

        assertEquals("a tiger, a tiger and a tiger", d.paragraph(1).text().plainText());
    }

    @Test
    void shorterReplacementInOneParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a tiger, a tiger and a tiger").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "tiger", new SubstringMatch()), "ox"));

        assertEquals("a ox, a ox and a ox", d.paragraph(1).text().plainText());
    }

    @Test
    void sameLengthReplacementInOneParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "dog"));

        assertEquals("dog dog dog", d.paragraph(1).text().plainText());
    }

    @Test
    void emptyReplacementDeletesEveryMatch() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "um, so um, yeah um").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "um", new WholeWordMatch()), ""));

        assertEquals(", so , yeah ", d.paragraph(1).text().plainText());
    }

    @Test
    void matchesRightNextToEachOther() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "abababab").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "ab", new SubstringMatch()), "[xyz]"));

        assertEquals("[xyz][xyz][xyz][xyz]", d.paragraph(1).text().plainText());
    }

    @Test
    void matchesAtTheStartAndTheEnd() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "end middle end").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "end", new SubstringMatch()), "edge"));

        assertEquals("edge middle edge", d.paragraph(1).text().plainText());
    }

    @Test
    void replacementThatContainsTheQueryDoesNotLoop() {
        // Matches are found first, so the new "cats" are not searched again.
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat and cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "cats"));

        assertEquals("cats and cats", d.paragraph(1).text().plainText());
    }

    @Test
    void wholeWordOnlyReplacesWholeWords() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat scattered the catnip").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new WholeWordMatch()), "dog"));

        assertEquals("the dog scattered the catnip", d.paragraph(1).text().plainText());
    }

    @Test
    void replacesInEveryParagraphAndSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "red and red").execute(d);
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "red").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "no match here").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "red, red, red").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "red", new SubstringMatch()), "blue"));

        assertEquals("blue and blue", d.paragraph(1).text().plainText());
        assertEquals("blue", d.paragraph(2).text().plainText());
        assertEquals("no match here", d.paragraph(3).text().plainText());
        assertEquals("blue, blue, blue", d.paragraph(4).text().plainText());
    }

    @Test
    void worksEvenIfTheMatchesAreNotInOrder() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "x x x").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> shuffled = new ArrayList<>(search.findAll(d, "x", new SubstringMatch()));
        Collections.reverse(shuffled);

        h.perform(search.replaceAllCommand(shuffled, "long"));

        assertEquals("long long long", d.paragraph(1).text().plainText());
    }

    @Test
    void replacementTakesTheFormattingOfTheTextItReplaces() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a cat and a cat").execute(d);
        d.paragraph(1).text().applyFormat(2, 5, FormatChange.bold(true)); // first "cat" bold
        CharFormat bold = d.paragraph(1).text().formatAt(2);
        CharFormat plain = d.paragraph(1).text().formatAt(0);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "tiger"));

        FormattedText t = d.paragraph(1).text(); // "a tiger and a tiger"
        assertEquals("a tiger and a tiger", t.plainText());
        assertEquals(bold, t.formatAt(2));   // first "tiger" is bold, like the "cat" it replaced
        assertEquals(bold, t.formatAt(6));
        assertEquals(plain, t.formatAt(14)); // second "tiger" stays plain
    }

    //one undo restores the original text exactly

    @Test
    void oneUndoRestoresTheOriginal() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a cat, a cat and a cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "tiger"));

        h.undo();

        assertEquals("a cat, a cat and a cat", d.paragraph(1).text().plainText());
        assertFalse(h.canUndo()); // the whole Replace All was one history entry
    }

    @Test
    void oneUndoRestoresAShorterReplacement() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a tiger, a tiger and a tiger").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "tiger", new SubstringMatch()), "ox"));

        h.undo();

        assertEquals("a tiger, a tiger and a tiger", d.paragraph(1).text().plainText());
    }

    @Test
    void oneUndoRestoresDeletedMatches() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "um, so um, yeah um").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "um", new WholeWordMatch()), ""));

        h.undo();

        assertEquals("um, so um, yeah um", d.paragraph(1).text().plainText());
    }

    @Test
    void oneUndoRestoresEveryParagraph() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "red and red").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "red, red, red").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "red", new SubstringMatch()), "a much longer word"));

        h.undo();

        assertEquals("red and red", d.paragraph(1).text().plainText());
        assertEquals("red, red, red", d.paragraph(2).text().plainText());
    }

    @Test
    void oneUndoRestoresTheFormattingExactly() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a cat and a cat").execute(d);
        d.paragraph(1).text().applyFormat(2, 5, FormatChange.bold(true));
        d.paragraph(1).text().applyFormat(12, 14, FormatChange.italic(true)); // "ca" of 2nd cat
        FormattedText before = d.paragraph(1).text().copy();
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "x"));

        h.undo();

        // FormattedText.equals compares the characters AND their formatting.
        assertEquals(before, d.paragraph(1).text());
    }

    @Test
    void redoAfterUndoReplacesEverythingAgain() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "lion"));

        h.undo();
        h.redo();

        assertEquals("lion lion lion", d.paragraph(1).text().plainText());
    }

    @Test
    void undoOnlyUndoesTheReplaceAllNotEarlierEdits() {
        Document d = new Document();
        History h = new History(d);
        h.perform(new AddSectionCommand(null, "S"));
        h.perform(new AddParagraphCommand(SectionPath.parse("1"), "cat cat"));
        SearchService search = new SearchService();
        h.perform(search.replaceAllCommand(
                search.findAll(d, "cat", new SubstringMatch()), "dog"));

        h.undo();

        assertEquals("cat cat", d.paragraph(1).text().plainText());
        assertTrue(h.canUndo()); // the add-paragraph is still there to undo
    }

    //no matches and bad input

    @Test
    void noMatchesGivesAnEmptyCommandThatIsNotPerformed() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "nothing to see").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        CompositeCommand all = search.replaceAllCommand(
                search.findAll(d, "zebra", new SubstringMatch()), "x");
        if (!all.isEmpty()) {
            h.perform(all); // what the Editor does: only perform when there is something
        }

        assertTrue(all.isEmpty());
        assertFalse(h.canUndo());
        assertEquals("nothing to see", d.paragraph(1).text().plainText());
    }

    @Test
    void emptySearchTextIsRejected() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "text").execute(d);
        SearchService search = new SearchService();

        EditException e = assertThrows(EditException.class,
                () -> search.findAll(d, "", new SubstringMatch()));

        assertEquals("Search text cannot be empty", e.getMessage());
    }

    @Test
    void replaceFirstOnlyChangesTheFirstMatch() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> matches = search.findAll(d, "cat", new SubstringMatch());

        h.perform(search.replaceFirstCommand(matches.get(0), "dog"));
        assertEquals("dog cat cat", d.paragraph(1).text().plainText());

        h.undo();
        assertEquals("cat cat cat", d.paragraph(1).text().plainText());
    }
}
