package edu.arizona.ece373.editor.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link WholeWordMatch} (6.8.1).
 *
 * <p>Covers the header's examples ("program" in "a program." yes, in "programs" no),
 * matches at the start and end of the string, matches next to punctuation, letters and
 * digits on either side, and the empty and null cases from the {@link MatchStrategy}
 * contract. Each match is a {start, end} pair, end exclusive.
 */
class WholeWordMatchTest {

    //the header's examples

    @Test
    void programMatchesInAProgramWithAPeriod() {
        List<int[]> m = new WholeWordMatch().findIn("a program.", "program");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {2, 9}, m.get(0));
    }

    @Test
    void programDoesNotMatchInPrograms() {
        assertTrue(new WholeWordMatch().findIn("programs", "program").isEmpty());
    }

    @Test
    void programDoesNotMatchInReprogram() {
        assertTrue(new WholeWordMatch().findIn("reprogram", "program").isEmpty());
    }

    //at the start and end of the string

    @Test
    void matchAtTheStartOfTheString() {
        List<int[]> m = new WholeWordMatch().findIn("program runs", "program");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 7}, m.get(0));
    }

    @Test
    void matchAtTheEndOfTheString() {
        List<int[]> m = new WholeWordMatch().findIn("run the program", "program");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {8, 15}, m.get(0));
    }

    @Test
    void queryIsTheWholeString() {
        List<int[]> m = new WholeWordMatch().findIn("program", "program");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 7}, m.get(0));
    }

    //next to punctuation

    @Test
    void matchesNextToPunctuationOnEitherSide() {
        String text = "(program), \"program\"; program! program?";
        List<int[]> m = new WholeWordMatch().findIn(text, "program");

        assertEquals(4, m.size());
        for (int[] pair : m) {
            assertEquals("program", text.substring(pair[0], pair[1]));
        }
    }

    @Test
    void hyphenAndApostropheCountAsBoundaries() {
        List<int[]> m = new WholeWordMatch().findIn("re-program the program's code", "program");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {3, 10}, m.get(0));
        assertArrayEquals(new int[] {15, 22}, m.get(1));
    }

    @Test
    void underscoreCountsAsABoundary() {
        // Decision in WholeWordMatch: only letters and digits are part of a word.
        List<int[]> m = new WholeWordMatch().findIn("cat_food", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 3}, m.get(0));
    }

    //letters and digits are part of the word

    @Test
    void aDigitNextToTheQueryMeansNoMatch() {
        assertTrue(new WholeWordMatch().findIn("cat2", "cat").isEmpty());
        assertTrue(new WholeWordMatch().findIn("2cat", "cat").isEmpty());
    }

    @Test
    void wordInsideAnotherWordIsSkipped() {
        assertTrue(new WholeWordMatch().findIn("the cat sat on a mat", "at").isEmpty());
    }

    @Test
    void skipsABadCandidateAndFindsTheRealWordAfterIt() {
        List<int[]> m = new WholeWordMatch().findIn("programs program", "program");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {9, 16}, m.get(0));
    }

    @Test
    void findsAWholeWordThatStartsInsideAFailedCandidate() {
        // "aa a": the "a" at 0 and 1 are inside "aa"; only the last one is a word.
        List<int[]> m = new WholeWordMatch().findIn("aa a", "a");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {3, 4}, m.get(0));
    }

    // several matches, case, empty and null

    @Test
    void severalWholeWordsLeftToRight() {
        List<int[]> m = new WholeWordMatch().findIn("a a a", "a");

        assertEquals(3, m.size());
        assertArrayEquals(new int[] {0, 1}, m.get(0));
        assertArrayEquals(new int[] {2, 3}, m.get(1));
        assertArrayEquals(new int[] {4, 5}, m.get(2));
    }

    @Test
    void multiWordQueryMatchesAsAPhrase() {
        List<int[]> m = new WholeWordMatch().findIn("the big cat and the big cats", "big cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {4, 11}, m.get(0));
    }

    @Test
    void isCaseSensitive() {
        List<int[]> m = new WholeWordMatch().findIn("The the THE", "the");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {4, 7}, m.get(0));
    }

    @Test
    void noMatchGivesAnEmptyList() {
        assertTrue(new WholeWordMatch().findIn("hello world", "zebra").isEmpty());
    }

    @Test
    void emptyTextGivesAnEmptyList() {
        assertTrue(new WholeWordMatch().findIn("", "a").isEmpty());
    }

    @Test
    void emptyQueryGivesAnEmptyList() {
        assertTrue(new WholeWordMatch().findIn("abc", "").isEmpty());
    }

    @Test
    void nullQueryOrTextGivesAnEmptyList() {
        assertTrue(new WholeWordMatch().findIn("abc", null).isEmpty());
        assertTrue(new WholeWordMatch().findIn(null, "a").isEmpty());
    }
}
