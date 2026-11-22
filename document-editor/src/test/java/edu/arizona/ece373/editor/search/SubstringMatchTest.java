package edu.arizona.ece373.editor.search;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link SubstringMatch} (6.8.1).
 *
 * <p>Covers matches at the start, middle and end, several matches, no overlaps
 * ("aaa" / "aa"), case sensitivity, and the empty and null cases from the
 * {@link MatchStrategy} contract. Each match is a {start, end} pair, end exclusive.
 */
class SubstringMatchTest {

    // start, middle, end

    @Test
    void matchAtTheStart() {
        List<int[]> m = new SubstringMatch().findIn("cat nap", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 3}, m.get(0));
    }

    @Test
    void matchInTheMiddle() {
        List<int[]> m = new SubstringMatch().findIn("a cat nap", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {2, 5}, m.get(0));
    }

    @Test
    void matchAtTheEnd() {
        List<int[]> m = new SubstringMatch().findIn("nap cat", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {4, 7}, m.get(0));
    }

    @Test
    void queryIsTheWholeText() {
        List<int[]> m = new SubstringMatch().findIn("cat", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 3}, m.get(0));
    }

    @Test
    void matchesInsideAWord() {
        // Substring matching does not care about word boundaries.
        List<int[]> m = new SubstringMatch().findIn("concatenate", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {3, 6}, m.get(0));
    }

    @Test
    void singleCharacterQuery() {
        List<int[]> m = new SubstringMatch().findIn("a.b.c", ".");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {1, 2}, m.get(0));
        assertArrayEquals(new int[] {3, 4}, m.get(1));
    }

    // several matches

    @Test
    void severalMatchesLeftToRight() {
        List<int[]> m = new SubstringMatch().findIn("the cat and the dog and the end", "the");

        assertEquals(3, m.size());
        assertArrayEquals(new int[] {0, 3}, m.get(0));
        assertArrayEquals(new int[] {12, 15}, m.get(1));
        assertArrayEquals(new int[] {24, 27}, m.get(2));
    }

    @Test
    void matchesRightNextToEachOther() {
        List<int[]> m = new SubstringMatch().findIn("abab", "ab");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {0, 2}, m.get(0));
        assertArrayEquals(new int[] {2, 4}, m.get(1));
    }

    @Test
    void eachPairIsTheMatchedText() {
        String text = "red, green, red";
        List<int[]> m = new SubstringMatch().findIn(text, "red");

        for (int[] pair : m) {
            assertEquals("red", text.substring(pair[0], pair[1]));
        }
        assertEquals(2, m.size());
    }

    // no overlaps

    @Test
    void aaaWithAaGivesOneMatchNotTwo() {
        List<int[]> m = new SubstringMatch().findIn("aaa", "aa");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {0, 2}, m.get(0));
    }

    @Test
    void aaaaWithAaGivesTwoMatchesNotThree() {
        List<int[]> m = new SubstringMatch().findIn("aaaa", "aa");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {0, 2}, m.get(0));
        assertArrayEquals(new int[] {2, 4}, m.get(1));
    }

    @Test
    void overlappingPatternContinuesAfterEachMatch() {
        List<int[]> m = new SubstringMatch().findIn("abababa", "aba");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {0, 3}, m.get(0));
        assertArrayEquals(new int[] {4, 7}, m.get(1));
    }

    //case, no match, empty and null

    @Test
    void isCaseSensitive() {
        List<int[]> m = new SubstringMatch().findIn("Cat cat CAT", "cat");

        assertEquals(1, m.size());
        assertArrayEquals(new int[] {4, 7}, m.get(0));
    }

    @Test
    void noMatchGivesAnEmptyList() {
        assertTrue(new SubstringMatch().findIn("hello world", "zebra").isEmpty());
    }

    @Test
    void queryLongerThanTheTextGivesNoMatch() {
        assertTrue(new SubstringMatch().findIn("cat", "cats").isEmpty());
    }

    @Test
    void emptyTextGivesAnEmptyList() {
        assertTrue(new SubstringMatch().findIn("", "a").isEmpty());
    }

    @Test
    void emptyQueryGivesAnEmptyList() {
        // Not a match at every position.
        assertTrue(new SubstringMatch().findIn("abc", "").isEmpty());
    }

    @Test
    void nullQueryOrTextGivesAnEmptyList() {
        assertTrue(new SubstringMatch().findIn("abc", null).isEmpty());
        assertTrue(new SubstringMatch().findIn(null, "a").isEmpty());
    }

    @Test
    void spacesCanBeSearchedFor() {
        List<int[]> m = new SubstringMatch().findIn("a  b", " ");

        assertEquals(2, m.size());
        assertArrayEquals(new int[] {1, 2}, m.get(0));
        assertArrayEquals(new int[] {2, 3}, m.get(1));
    }
}
