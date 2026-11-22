package edu.arizona.ece373.editor.search;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.history.InsertTextCommand;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.SectionPath;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.view.BalancedLineBreaker;
import edu.arizona.ece373.editor.view.DocumentStatistics;
import edu.arizona.ece373.editor.view.GreedyLineBreaker;
import edu.arizona.ece373.editor.view.LineBreaker;
import edu.arizona.ece373.editor.view.PlainTextRenderer;
import edu.arizona.ece373.editor.view.Statistics;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests performance on a 100,000-character document (6.9.8).
 *
 * <p>Each test builds a document of exactly 100,000 characters: 10 sections of 100
 * paragraphs, each paragraph 100 characters (the sentence below twice, plus 10 letters).
 * Then one operation (insert, find, Replace All or stats) must finish in under a
 * second. Building the document is not timed.
 *
 * <p>Each paragraph contains "fox" twice and the whole word "the" four times, so the
 * whole document has 2,000 and 4,000 of them.
 */
class LargeDocumentTest {

    private static final String SENTENCE = "the quick brown fox jumps over the lazy dog. ";
    private static final String PARAGRAPH = SENTENCE + SENTENCE + "abcdefghij"; // 100 chars
    private static final int SECTIONS = 10;
    private static final int PARAGRAPHS_PER_SECTION = 100;
    private static final Duration LIMIT = Duration.ofSeconds(1);

    /** Builds the 100,000-character document (not timed). */
    private static Document bigDocument() {
        Document d = new Document();
        for (int s = 1; s <= SECTIONS; s++) {
            new AddSectionCommand(null, "Section " + s).execute(d);
            for (int p = 0; p < PARAGRAPHS_PER_SECTION; p++) {
                new AddParagraphCommand(SectionPath.parse(String.valueOf(s)), PARAGRAPH).execute(d);
            }
        }
        return d;
    }

    //the document really is ~100,000 characters

    @Test
    void documentHasOneHundredThousandCharacters() {
        Document d = bigDocument();
        DocumentStatistics stats = new DocumentStatistics(d);

        Statistics s = stats.current();

        assertEquals(100_000, s.characters());
        assertEquals(1_000, s.paragraphs());
        assertEquals(19_000, s.words()); // 9 + 9 + "abcdefghij" = 19 per paragraph
    }

    // ---- insert ----

    @Test
    void oneInsertInTheMiddleIsFast() {
        Document d = bigDocument();
        History h = new History(d);

        assertTimeout(LIMIT, () -> h.perform(new InsertTextCommand(500, 50, "INSERTED ")));

        assertTrue(d.paragraph(500).text().plainText().contains("INSERTED "));
    }

    @Test
    void aThousandInsertsAreFast() {
        Document d = bigDocument();
        History h = new History(d);

        assertTimeout(LIMIT, () -> {
            for (int i = 1; i <= 1_000; i++) {
                h.perform(new InsertTextCommand(i, 0, "x"));
            }
        });

        assertEquals(101, d.paragraph(1_000).text().length());
    }

    @Test
    void undoingAThousandInsertsIsFast() {
        Document d = bigDocument();
        History h = new History(d);
        for (int i = 1; i <= 1_000; i++) {
            h.perform(new InsertTextCommand(i, 0, "x"));
        }

        assertTimeout(LIMIT, () -> {
            for (int i = 0; i < 1_000; i++) {
                h.undo();
            }
        });

        assertEquals(PARAGRAPH, d.paragraph(1).text().plainText());
        assertEquals(PARAGRAPH, d.paragraph(1_000).text().plainText());
    }

    //find

    @Test
    void substringFindIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        int[] found = new int[1];

        assertTimeout(LIMIT, () -> found[0] = search.findAll(d, "fox", new SubstringMatch()).size());

        assertEquals(2_000, found[0]);
    }

    @Test
    void wholeWordFindIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        int[] found = new int[1];

        assertTimeout(LIMIT, () -> found[0] = search.findAll(d, "the", new WholeWordMatch()).size());

        assertEquals(4_000, found[0]);
    }

    @Test
    void findWithNoMatchesIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        int[] found = new int[1];

        assertTimeout(LIMIT, () -> found[0] = search.findAll(d, "zebra", new SubstringMatch()).size());

        assertEquals(0, found[0]);
    }

    //Replace All

    @Test
    void replaceAllIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> matches = search.findAll(d, "fox", new SubstringMatch());
        CompositeCommand all = search.replaceAllCommand(matches, "cat");

        assertTimeout(LIMIT, () -> h.perform(all));

        assertTrue(search.findAll(d, "fox", new SubstringMatch()).isEmpty());
        assertEquals(2_000, search.findAll(d, "cat", new SubstringMatch()).size());
    }

    @Test
    void replaceAllWithLongerTextIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        History h = new History(d);
        List<Match> matches = search.findAll(d, "the", new WholeWordMatch());
        CompositeCommand all = search.replaceAllCommand(matches, "a very long replacement");

        assertTimeout(LIMIT, () -> h.perform(all));

        assertEquals(4_000,
                search.findAll(d, "a very long replacement", new SubstringMatch()).size());
    }

    @Test
    void undoingAReplaceAllIsFast() {
        Document d = bigDocument();
        SearchService search = new SearchService();
        History h = new History(d);
        h.perform(search.replaceAllCommand(
                search.findAll(d, "fox", new SubstringMatch()), "cat"));

        assertTimeout(LIMIT, h::undo);

        assertEquals(2_000, search.findAll(d, "fox", new SubstringMatch()).size());
        assertEquals(PARAGRAPH, d.paragraph(777).text().plainText());
    }

    //stats

    @Test
    void statsRecountIsFast() {
        Document d = bigDocument();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        Statistics[] result = new Statistics[1];

        assertTimeout(LIMIT, () -> result[0] = stats.current());

        assertEquals(100_000, result[0].characters());
    }

    @Test
    void statsAfterManyEditsIsFast() {
        Document d = bigDocument();
        DocumentStatistics stats = new DocumentStatistics(d);
        d.addListener(stats);
        History h = new History(d);
        stats.current();
        for (int i = 1; i <= 1_000; i++) {
            h.perform(new InsertTextCommand(i, 0, "x "));
        }
        Statistics[] result = new Statistics[1];

        // 1,000 edits only marked the counts stale; this is the one recount.
        assertTimeout(LIMIT, () -> result[0] = stats.current());

        assertEquals(102_000, result[0].characters());
        assertEquals(20_000, result[0].words()); // each "x " added one word
    }

    //line breaking (both breakers), the expensive part of rendering

    /** Every word in the document, split on whitespace like PlainTextRenderer does. */
    private static List<String> allWords(Document d) {
        List<Paragraph> paragraphs = new ArrayList<>();
        d.section(null).collectParagraphs(paragraphs);
        List<String> words = new ArrayList<>();
        for (Paragraph p : paragraphs) {
            for (String w : p.text().plainText().split("\\s+")) {
                if (!w.isEmpty()) {
                    words.add(w);
                }
            }
        }
        return words;
    }

    /** Breaks each paragraph separately, the way a renderer lays out a document. */
    private static int breakEveryParagraph(Document d, LineBreaker breaker, int width) {
        List<Paragraph> paragraphs = new ArrayList<>();
        d.section(null).collectParagraphs(paragraphs);
        int lines = 0;
        for (Paragraph p : paragraphs) {
            List<String> words = new ArrayList<>();
            for (String w : p.text().plainText().split("\\s+")) {
                if (!w.isEmpty()) {
                    words.add(w);
                }
            }
            lines += breaker.breakLines(words, width).size();
        }
        return lines;
    }

    @Test
    void greedyBreakingEveryParagraphIsFast() {
        Document d = bigDocument();
        int[] lines = new int[1];

        assertTimeout(LIMIT, () -> lines[0] = breakEveryParagraph(d, new GreedyLineBreaker(), 40));

        assertTrue(lines[0] >= 1_000 * 3); // 100 chars at width 40 needs at least 3 lines
    }

    @Test
    void balancedBreakingEveryParagraphIsFast() {
        Document d = bigDocument();
        int[] lines = new int[1];

        assertTimeout(LIMIT, () -> lines[0] = breakEveryParagraph(d, new BalancedLineBreaker(), 40));

        assertTrue(lines[0] >= 1_000 * 3);
    }

    @Test
    void greedyBreakingTheWholeDocumentAsOneListIsFast() {
        List<String> words = allWords(bigDocument());
        assertEquals(19_000, words.size());
        int[] longest = new int[1];

        assertTimeout(LIMIT, () -> {
            for (String line : new GreedyLineBreaker().breakLines(words, 80)) {
                longest[0] = Math.max(longest[0], line.length());
            }
        });

        assertTrue(longest[0] <= 80);
    }

    @Test
    void balancedBreakingTheWholeDocumentAsOneListIsFast() {
        // 19,000 words in one go: checks the O(n * k) claim in BalancedLineBreaker.
        List<String> words = allWords(bigDocument());
        int[] count = new int[1];
        int[] longest = new int[1];

        assertTimeout(LIMIT, () -> {
            List<String> lines = new BalancedLineBreaker().breakLines(words, 80);
            count[0] = lines.size();
            for (String line : lines) {
                longest[0] = Math.max(longest[0], line.length());
            }
        });

        assertTrue(longest[0] <= 80);
        assertTrue(count[0] > 0);
    }

    //full render (both breakers)

    @Test
    void plainRenderWithGreedyBreakerIsFast() {
        Document d = bigDocument();
        String[] out = new String[1];

        assertTimeout(LIMIT, () -> out[0] = new PlainTextRenderer().render(d, new GreedyLineBreaker(), 80));

        String[] lines = out[0].split("\n", -1);
        assertEquals("1 Section 1", lines[0]);
        assertTrue(out[0].contains("10 Section 10"));
        for (String line : lines) {
            assertTrue(line.length() <= 80, line);
        }
    }

    @Test
    void plainRenderWithBalancedBreakerIsFast() {
        Document d = bigDocument();
        String[] out = new String[1];

        assertTimeout(LIMIT, () -> out[0] = new PlainTextRenderer().render(d, new BalancedLineBreaker(), 80));

        String[] lines = out[0].split("\n", -1);
        assertEquals("1 Section 1", lines[0]);
        assertTrue(out[0].contains("10 Section 10"));
        for (String line : lines) {
            assertTrue(line.length() <= 80, line);
        }
    }

    @Test
    void renderingAtANarrowWidthIsStillFast() {
        // Width 20 means many more lines (and much more work for the balanced breaker).
        Document d = bigDocument();
        String[] out = new String[1];

        assertTimeout(LIMIT, () -> out[0] = new PlainTextRenderer().render(d, new BalancedLineBreaker(), 20));

        for (String line : out[0].split("\n", -1)) {
            assertTrue(line.length() <= 20, line);
        }
    }
}
