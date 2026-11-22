package edu.arizona.ece373.editor.search;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.TextRange;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SearchService} find and replace-first (6.8.1, 6.8.2), and {@link Match}.
 *
 * <p>Covers matches listed in document order across sections, replace-first changing
 * only the first match, the empty-query error, and no matches adding nothing to the
 * history. Replace All itself is tested in {@link ReplaceAllTest}.
 */
class SearchServiceTest {

    //matches listed in document order across sections

    @Test
    void findsEveryMatchInOneParagraphLeftToRight() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "the cat and the dog").execute(d);
        SearchService search = new SearchService();

        List<Match> found = search.findAll(d, "the", new SubstringMatch());

        assertEquals(List.of(
                new Match(new TextRange(1, 0, 3)),
                new Match(new TextRange(1, 12, 15))), found);
    }

    @Test
    void matchesAreInDocumentOrderAcrossSections() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "x").execute(d);      // p1
        new AddParagraphCommand(SectionPath.parse("1"), "no").execute(d);     // p2
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "x x").execute(d);    // p3
        new AddSectionCommand(null, "Three").execute(d);
        new AddParagraphCommand(SectionPath.parse("3"), "..x").execute(d);    // p4
        SearchService search = new SearchService();

        List<Match> found = search.findAll(d, "x", new SubstringMatch());

        assertEquals(List.of(
                new Match(new TextRange(1, 0, 1)),
                new Match(new TextRange(3, 0, 1)),
                new Match(new TextRange(3, 2, 3)),
                new Match(new TextRange(4, 2, 3))), found);
    }

    @Test
    void subsectionsAreSearchedInDocumentOrder() {
        // Section 1 has a paragraph, then subsection 1.1, then another paragraph.
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "first x").execute(d);   // p1
        new AddSectionCommand(SectionPath.parse("1"), "One.One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1.1"), "inner x").execute(d); // p2
        new AddParagraphCommand(SectionPath.parse("1"), "last x").execute(d);    // p3 (after 1.1)
        SearchService search = new SearchService();

        List<Match> found = search.findAll(d, "x", new SubstringMatch());

        assertEquals(3, found.size());
        assertEquals(1, found.get(0).range.paragraph);
        assertEquals(2, found.get(1).range.paragraph);
        assertEquals(3, found.get(2).range.paragraph);
        assertEquals("p2:6-7", found.get(1).toString());
    }

    @Test
    void sectionTitlesAreNotSearched() {
        Document d = new Document();
        new AddSectionCommand(null, "the title").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "no match in here").execute(d);
        SearchService search = new SearchService();

        assertTrue(search.findAll(d, "title", new SubstringMatch()).isEmpty());
    }

    @Test
    void emptyParagraphsAreSkippedButStillCounted() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "").execute(d);       // p1
        new AddParagraphCommand(SectionPath.parse("1"), "find me").execute(d); // p2
        SearchService search = new SearchService();

        List<Match> found = search.findAll(d, "me", new SubstringMatch());

        assertEquals(List.of(new Match(new TextRange(2, 5, 7))), found);
    }

    @Test
    void theStrategyDecidesWhatCounts() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat scattered cat").execute(d);
        SearchService search = new SearchService();

        assertEquals(3, search.findAll(d, "cat", new SubstringMatch()).size());
        assertEquals(2, search.findAll(d, "cat", new WholeWordMatch()).size());
    }

    @Test
    void searchIsCaseSensitive() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "Cat cat CAT").execute(d);
        SearchService search = new SearchService();

        List<Match> found = search.findAll(d, "cat", new SubstringMatch());

        assertEquals(List.of(new Match(new TextRange(1, 4, 7))), found);
    }

    @Test
    void findDoesNotChangeTheDocumentOrFireEvents() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat").execute(d);
        int[] events = {0};
        d.addListener(e -> events[0]++);
        SearchService search = new SearchService();

        search.findAll(d, "cat", new SubstringMatch());

        assertEquals("cat cat", d.paragraph(1).text().plainText());
        assertEquals(0, events[0]);
    }

    @Test
    void emptyDocumentHasNoMatches() {
        SearchService search = new SearchService();

        assertTrue(search.findAll(new Document(), "x", new SubstringMatch()).isEmpty());
    }

    //Match

    @Test
    void matchPrintsTheSameSyntaxTheUserTypes() {
        assertEquals("p2:31-38", new Match(new TextRange(2, 31, 38)).toString());
    }

    @Test
    void matchesWithTheSameRangeAreEqual() {
        assertEquals(new Match(new TextRange(2, 31, 38)), new Match(new TextRange(2, 31, 38)));
        assertEquals(new Match(new TextRange(2, 31, 38)).hashCode(),
                new Match(new TextRange(2, 31, 38)).hashCode());
        assertNotEquals(new Match(new TextRange(2, 31, 38)), new Match(new TextRange(3, 31, 38)));
        assertNotEquals(new Match(new TextRange(2, 31, 38)), new Match(new TextRange(2, 30, 38)));
        assertNotEquals(new Match(new TextRange(2, 31, 38)), new Match(new TextRange(2, 31, 39)));
    }

    @Test
    void matchRejectsANullRange() {
        assertThrows(NullPointerException.class, () -> new Match(null));
    }

    //replace-first changes only the first match

    @Test
    void replaceFirstChangesOnlyTheFirstMatch() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> found = search.findAll(d, "cat", new SubstringMatch());

        h.perform(search.replaceFirstCommand(found.get(0), "dog"));

        assertEquals("dog cat cat", d.paragraph(1).text().plainText());
    }

    @Test
    void replaceFirstWhenTheFirstMatchIsInALaterSection() {
        Document d = new Document();
        new AddSectionCommand(null, "One").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "nothing here").execute(d);
        new AddSectionCommand(null, "Two").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "cat and cat").execute(d);
        new AddParagraphCommand(SectionPath.parse("2"), "cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> found = search.findAll(d, "cat", new SubstringMatch());

        h.perform(search.replaceFirstCommand(found.get(0), "dog"));

        assertEquals("nothing here", d.paragraph(1).text().plainText());
        assertEquals("dog and cat", d.paragraph(2).text().plainText());
        assertEquals("cat", d.paragraph(3).text().plainText());
    }

    @Test
    void replaceFirstWithLongerAndShorterText() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceFirstCommand(
                search.findAll(d, "cat", new SubstringMatch()).get(0), "tiger"));
        assertEquals("tiger cat", d.paragraph(1).text().plainText());

        h.perform(search.replaceFirstCommand(
                search.findAll(d, "tiger", new SubstringMatch()).get(0), "ox"));
        assertEquals("ox cat", d.paragraph(1).text().plainText());
    }

    @Test
    void replacingFirstAgainMovesOnToTheNextMatch() {
        // After the first one is replaced, a new search finds the old second match first.
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        for (int i = 0; i < 2; i++) {
            Match first = search.findAll(d, "cat", new SubstringMatch()).get(0);
            h.perform(search.replaceFirstCommand(first, "dog"));
        }

        assertEquals("dog dog cat", d.paragraph(1).text().plainText());
    }

    @Test
    void replaceFirstIsOneUndo() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "cat cat cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceFirstCommand(
                search.findAll(d, "cat", new SubstringMatch()).get(0), "dog"));

        h.undo();

        assertEquals("cat cat cat", d.paragraph(1).text().plainText());
        assertFalse(h.canUndo());
    }

    @Test
    void replaceFirstWithNullDeletesTheMatch() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a big cat").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        h.perform(search.replaceFirstCommand(
                search.findAll(d, "big ", new SubstringMatch()).get(0), null));

        assertEquals("a cat", d.paragraph(1).text().plainText());
    }

    @Test
    void replaceFirstRejectsANullMatch() {
        SearchService search = new SearchService();

        assertThrows(NullPointerException.class, () -> search.replaceFirstCommand(null, "x"));
    }

    //empty query error

    @Test
    void emptyQueryIsAnError() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "text").execute(d);
        SearchService search = new SearchService();

        EditException e = assertThrows(EditException.class,
                () -> search.findAll(d, "", new SubstringMatch()));

        assertEquals("Search text cannot be empty", e.getMessage());
    }

    @Test
    void nullQueryIsAnError() {
        Document d = new Document();
        SearchService search = new SearchService();

        assertThrows(EditException.class, () -> search.findAll(d, null, new WholeWordMatch()));
    }

    @Test
    void aQueryOfOnlySpacesIsAllowed() {
        // Only the EMPTY query is an error; spaces are real characters to search for.
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "a  b").execute(d);
        SearchService search = new SearchService();

        assertEquals(List.of(new Match(new TextRange(1, 1, 3))),
                search.findAll(d, "  ", new SubstringMatch()));
    }

    //o matches adds nothing to the history

    @Test
    void noMatchesGivesAnEmptyList() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "nothing to see").execute(d);
        SearchService search = new SearchService();

        assertTrue(search.findAll(d, "zebra", new SubstringMatch()).isEmpty());
    }

    @Test
    void noMatchesAddsNothingToTheHistory() {
        Document d = new Document();
        new AddSectionCommand(null, "S").execute(d);
        new AddParagraphCommand(SectionPath.parse("1"), "nothing to see").execute(d);
        SearchService search = new SearchService();
        History h = new History(d);

        CompositeCommand all = search.replaceAllCommand(
                search.findAll(d, "zebra", new SubstringMatch()), "x");
        if (!all.isEmpty()) {
            h.perform(all); // the Editor only performs a non-empty Replace All
        }

        assertTrue(all.isEmpty());
        assertFalse(h.canUndo());
        assertEquals("nothing to see", d.paragraph(1).text().plainText());
    }
}
