package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link BalancedLineBreaker}: a paragraph where balanced differs from greedy and is
 * more even; long word, single word, empty input; speed on a large paragraph.
 *
 * <p>Satisfies 6.6.6, 6.9.8.
 */
class BalancedLineBreakerTest {

    private final LineBreaker balanced = new BalancedLineBreaker();
    private final LineBreaker greedy = new GreedyLineBreaker();

    private static List<String> words(String text) {
        return Arrays.asList(text.split(" "));
    }

    /** Sum of squared slack over every line but the last: what balanced minimises. */
    private static long raggedness(List<String> lines, int width) {
        long sum = 0;
        for (int i = 0; i < lines.size() - 1; i++) {
            long slack = width - lines.get(i).length();
            sum += slack * slack;
        }
        return sum;
    }

    @Test
    void differsFromGreedyAndIsMoreEven() {
        List<String> w = words("aaa bb cc ddddd");
        List<String> g = greedy.breakLines(w, 6);
        List<String> b = balanced.breakLines(w, 6);
        assertEquals(List.of("aaa bb", "cc", "ddddd"), g);   // slack 0, 4 -> cost 16
        assertEquals(List.of("aaa", "bb cc", "ddddd"), b);   // slack 3, 1 -> cost 10
        assertTrue(raggedness(b, 6) < raggedness(g, 6));
    }

    @Test
    void longerParagraphIsMoreEvenThanGreedy() {
        List<String> w = words("We need to make sure the text is laid out so that every "
                + "line is roughly the same length and nothing looks too ragged at all");
        List<String> g = greedy.breakLines(w, 16);
        List<String> b = balanced.breakLines(w, 16);
        assertNotEquals(g, b);
        assertTrue(raggedness(b, 16) < raggedness(g, 16));
    }

    @Test
    void neverWorseThanGreedy() {
        Random r = new Random(373);
        for (int trial = 0; trial < 200; trial++) {
            List<String> w = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                w.add("x".repeat(1 + r.nextInt(8)));
            }
            int width = 10 + r.nextInt(30);
            assertTrue(raggedness(balanced.breakLines(w, width), width)
                    <= raggedness(greedy.breakLines(w, width), width));
        }
    }

    @Test
    void keepsEveryWordInOrderAndNoLineTooWide() {
        String text = "the quick brown fox jumps over the lazy dog again and again";
        for (int width = 5; width <= 30; width++) {
            List<String> lines = balanced.breakLines(words(text), width);
            assertEquals(text, String.join(" ", lines));
            for (String line : lines) {
                assertTrue(line.length() <= width, line + " wider than " + width);
            }
        }
    }

    @Test
    void longWordGoesAloneAndIsNotSplit() {
        List<String> lines = balanced.breakLines(words("a abcdefghij b c"), 5);
        assertTrue(lines.contains("abcdefghij"), lines.toString());
        assertEquals("a abcdefghij b c", String.join(" ", lines));
    }

    @Test
    void singleLongWord() {
        assertEquals(List.of("abcdefghij"), balanced.breakLines(List.of("abcdefghij"), 3));
    }

    @Test
    void singleWord() {
        assertEquals(List.of("hi"), balanced.breakLines(List.of("hi"), 10));
    }

    @Test
    void exactFitStaysOnOneLine() {
        assertEquals(List.of("aaa bb"), balanced.breakLines(words("aaa bb"), 6));
    }

    @Test
    void emptyInputGivesNoLines() {
        assertTrue(balanced.breakLines(List.of(), 10).isEmpty());
    }

    @Test
    void fastOnHundredThousandCharacters() {
        Random r = new Random(1);
        List<String> w = new ArrayList<>();
        int chars = 0;
        while (chars < 100_000) {
            String word = "w".repeat(1 + r.nextInt(10));
            w.add(word);
            chars += word.length() + 1;
        }
        List<String> lines = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> balanced.breakLines(w, 80));
        assertEquals(w.size(), String.join(" ", lines).split(" ").length);
    }
}
