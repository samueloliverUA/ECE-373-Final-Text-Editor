package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link GreedyLineBreaker}: exact fit, one character over, a single long word, empty
 * input.
 *
 * <p>Satisfies 6.6.5.
 */
class GreedyLineBreakerTest {

    private final LineBreaker greedy = new GreedyLineBreaker();

    private static List<String> words(String text) {
        return Arrays.asList(text.split(" "));
    }

    @Test
    void exactFitStaysOnOneLine() {
        // "aaa bb" is exactly 6 characters
        assertEquals(List.of("aaa bb"), greedy.breakLines(words("aaa bb"), 6));
    }

    @Test
    void oneCharacterOverBreaks() {
        assertEquals(List.of("aaa", "bb"), greedy.breakLines(words("aaa bb"), 5));
    }

    @Test
    void fillsEachLineBeforeStartingNext() {
        assertEquals(List.of("aaa bb", "cc", "ddddd"),
                greedy.breakLines(words("aaa bb cc ddddd"), 6));
    }

    @Test
    void longWordGoesAloneAndIsNotSplit() {
        assertEquals(List.of("a", "abcdefghij", "b c"),
                greedy.breakLines(words("a abcdefghij b c"), 5));
    }

    @Test
    void singleLongWord() {
        assertEquals(List.of("abcdefghij"), greedy.breakLines(List.of("abcdefghij"), 3));
    }

    @Test
    void singleWord() {
        assertEquals(List.of("hi"), greedy.breakLines(List.of("hi"), 10));
    }

    @Test
    void emptyInputGivesNoLines() {
        assertTrue(greedy.breakLines(List.of(), 10).isEmpty());
    }

    @Test
    void wordsSeparatedByOneSpaceAndNoLineTooWide() {
        String text = "the quick brown fox jumps over the lazy dog again and again";
        for (int width = 5; width <= 30; width++) {
            List<String> lines = greedy.breakLines(words(text), width);
            assertEquals(text, String.join(" ", lines));
            for (String line : lines) {
                assertTrue(line.length() <= width, line + " wider than " + width);
            }
        }
    }
}
