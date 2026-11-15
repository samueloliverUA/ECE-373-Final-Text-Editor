package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link FormattedText} and {@link TextRun}: insert, cut, splitAt and append at the
 * start, middle and end and on an empty text; formatting kept on inserted and remaining
 * characters; applyFormat across several runs with merging and no empty runs; bad offsets.
 *
 * <p>Satisfies 6.3.x, 6.5.2, 6.5.3.
 */
class FormattedTextTest {

    private static final CharFormat PLAIN = CharFormat.DEFAULT;
    private static final CharFormat BOLD = PLAIN.with(FormatChange.bold(true));
    private static final CharFormat ITALIC = PLAIN.with(FormatChange.italic(true));

    /** "hello " plain + "big" bold + " world" italic. */
    private static FormattedText mixed() {
        return new FormattedText(List.of(
                new TextRun("hello ", PLAIN),
                new TextRun("big", BOLD),
                new TextRun(" world", ITALIC)));
    }

    /** Checks the invariants: no empty runs, no equal neighbours. */
    private static void assertNormalized(FormattedText t) {
        List<TextRun> runs = t.runs();
        for (int i = 0; i < runs.size(); i++) {
            assertFalse(runs.get(i).text.isEmpty(), "empty run at " + i + " in " + runs);
            if (i > 0) {
                assertNotEquals(runs.get(i - 1).format, runs.get(i).format,
                        "unmerged neighbours at " + i + " in " + runs);
            }
        }
    }

    private static void assertBad(Runnable r) {
        assertThrows(EditException.class, r::run);
    }

    // ---------------------------------------------------------------------------------------
    // TextRun
    // ---------------------------------------------------------------------------------------

    @Nested
    class TextRunTests {

        @Test
        void holdsTextAndFormat() {
            TextRun r = new TextRun("abc", BOLD);
            assertEquals("abc", r.text);
            assertEquals(BOLD, r.format);
            assertEquals(3, r.length());
        }

        @Test
        void withersReturnNewRuns() {
            TextRun r = new TextRun("abc", BOLD);
            assertEquals(new TextRun("xy", BOLD), r.withText("xy"));
            assertEquals(new TextRun("abc", ITALIC), r.withFormat(ITALIC));
            assertEquals("abc", r.text);
            assertEquals(BOLD, r.format);
        }

        @Test
        void equalsComparesTextAndFormat() {
            assertEquals(new TextRun("a", BOLD), new TextRun("a", BOLD));
            assertEquals(new TextRun("a", BOLD).hashCode(), new TextRun("a", BOLD).hashCode());
            assertNotEquals(new TextRun("a", BOLD), new TextRun("a", ITALIC));
            assertNotEquals(new TextRun("a", BOLD), new TextRun("b", BOLD));
        }

        @Test
        void rejectsNulls() {
            assertThrows(NullPointerException.class, () -> new TextRun(null, BOLD));
            assertThrows(NullPointerException.class, () -> new TextRun("a", null));
        }
    }

    // ---------------------------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------------------------

    @Nested
    class Construction {

        @Test
        void plainTextIsOneDefaultRun() {
            FormattedText t = new FormattedText("hello");
            assertEquals(List.of(new TextRun("hello", PLAIN)), t.runs());
            assertEquals(5, t.length());
            assertEquals("hello", t.plainText());
        }

        @Test
        void emptyAndNullHaveNoRuns() {
            assertTrue(new FormattedText("").runs().isEmpty());
            assertTrue(new FormattedText((String) null).isEmpty());
            assertEquals(0, new FormattedText("").length());
        }

        @Test
        void runListIsNormalized() {
            FormattedText t = new FormattedText(List.of(
                    new TextRun("a", BOLD), new TextRun("", ITALIC), new TextRun("b", BOLD)));
            assertEquals(List.of(new TextRun("ab", BOLD)), t.runs());
        }

        @Test
        void runsListIsReadOnly() {
            assertThrows(UnsupportedOperationException.class,
                    () -> mixed().runs().add(new TextRun("x", PLAIN)));
        }

        @Test
        void copyIsIndependent() {
            FormattedText t = mixed();
            FormattedText c = t.copy();
            assertEquals(t, c);
            c.insert(0, "X");
            assertEquals("hello big world", t.plainText());
            assertEquals("Xhello big world", c.plainText());
        }

        @Test
        void formatAtEachCharacter() {
            FormattedText t = mixed();
            assertEquals(PLAIN, t.formatAt(0));
            assertEquals(PLAIN, t.formatAt(5));
            assertEquals(BOLD, t.formatAt(6));
            assertEquals(BOLD, t.formatAt(8));
            assertEquals(ITALIC, t.formatAt(9));
            assertEquals(ITALIC, t.formatAt(14));
            assertBad(() -> t.formatAt(15));
            assertBad(() -> t.formatAt(-1));
        }
    }

    // ---------------------------------------------------------------------------------------
    // insert
    // ---------------------------------------------------------------------------------------

    @Nested
    class Insert {

        @Test
        void atStartMiddleEnd() {
            FormattedText t = new FormattedText("bd");
            t.insert(0, "a");
            t.insert(2, "c");
            t.insert(4, "e");
            assertEquals("abcde", t.plainText());
            assertEquals(1, t.runs().size());
        }

        @Test
        void intoEmptyTextGetsDefault() {
            FormattedText t = new FormattedText("");
            t.insert(0, "hi");
            assertEquals(List.of(new TextRun("hi", PLAIN)), t.runs());
        }

        @Test
        void takesFormatOfCharacterBefore() {
            FormattedText t = mixed();
            t.insert(9, "!"); // right after "big"
            assertEquals("hello big! world", t.plainText());
            assertEquals(BOLD, t.formatAt(9));
            assertEquals(ITALIC, t.formatAt(10));
            assertNormalized(t);
        }

        @Test
        void atStartTakesFormatOfFirstCharacter() {
            FormattedText t = new FormattedText("bc", BOLD);
            t.insert(0, "a");
            assertEquals(List.of(new TextRun("abc", BOLD)), t.runs());
        }

        @Test
        void insideRunKeepsSurroundingFormat() {
            FormattedText t = mixed();
            t.insert(7, "XX"); // inside "big"
            assertEquals("hello bXXig world", t.plainText());
            assertEquals(List.of(new TextRun("hello ", PLAIN), new TextRun("bXXig", BOLD),
                    new TextRun(" world", ITALIC)), t.runs());
        }

        @Test
        void formattedInsertKeepsItsFormatting() {
            FormattedText t = new FormattedText("ac");
            t.insert(1, new FormattedText("b", BOLD));
            assertEquals(List.of(new TextRun("a", PLAIN), new TextRun("b", BOLD),
                    new TextRun("c", PLAIN)), t.runs());
        }

        @Test
        void formattedInsertMergesWithNeighbours() {
            FormattedText t = new FormattedText("ac");
            t.insert(1, new FormattedText("b"));
            assertEquals(List.of(new TextRun("abc", PLAIN)), t.runs());
        }

        @Test
        void emptyInsertChangesNothing() {
            FormattedText t = mixed();
            t.insert(3, "");
            assertEquals(mixed(), t);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 4, 100})
        void badOffsetRejectedAndTextUnchanged(int offset) {
            FormattedText t = new FormattedText("abc");
            assertBad(() -> t.insert(offset, "x"));
            assertEquals("abc", t.plainText());
        }
    }

    // ---------------------------------------------------------------------------------------
    // cut
    // ---------------------------------------------------------------------------------------

    @Nested
    class Cut {

        @Test
        void fromStart() {
            FormattedText t = new FormattedText("abcdef");
            FormattedText out = t.cut(0, 2);
            assertEquals("ab", out.plainText());
            assertEquals("cdef", t.plainText());
        }

        @Test
        void fromMiddle() {
            FormattedText t = new FormattedText("abcdef");
            assertEquals("cd", t.cut(2, 4).plainText());
            assertEquals("abef", t.plainText());
        }

        @Test
        void fromEnd() {
            FormattedText t = new FormattedText("abcdef");
            assertEquals("ef", t.cut(4, 6).plainText());
            assertEquals("abcd", t.plainText());
        }

        @Test
        void everything() {
            FormattedText t = mixed();
            FormattedText out = t.cut(0, t.length());
            assertEquals(mixed(), out);
            assertTrue(t.isEmpty());
        }

        @Test
        void emptyRangeOnEmptyText() {
            FormattedText t = new FormattedText("");
            assertTrue(t.cut(0, 0).isEmpty());
            assertTrue(t.isEmpty());
        }

        @Test
        void acrossRunsKeepsFormattingOnBothSides() {
            FormattedText t = mixed();
            FormattedText out = t.cut(4, 11); // "o big w"
            assertEquals("o big w", out.plainText());
            assertEquals(List.of(new TextRun("o ", PLAIN), new TextRun("big", BOLD),
                    new TextRun(" w", ITALIC)), out.runs());
            assertEquals(List.of(new TextRun("hell", PLAIN), new TextRun("orld", ITALIC)),
                    t.runs());
        }

        @Test
        void removingMiddleRunMergesEqualNeighbours() {
            FormattedText t = new FormattedText(List.of(new TextRun("aa", PLAIN),
                    new TextRun("BB", BOLD), new TextRun("cc", PLAIN)));
            t.cut(2, 4);
            assertEquals(List.of(new TextRun("aacc", PLAIN)), t.runs());
        }

        @Test
        void cutThenInsertRestoresOriginal() {
            FormattedText t = mixed();
            FormattedText out = t.cut(3, 12);
            t.insert(3, out);
            assertEquals(mixed(), t);
            assertNormalized(t);
        }

        @ParameterizedTest
        @CsvSource({"-1, 2", "0, 4", "3, 2", "4, 4"})
        void badRangeRejectedAndTextUnchanged(int start, int end) {
            FormattedText t = new FormattedText("abc");
            assertBad(() -> t.cut(start, end));
            assertEquals("abc", t.plainText());
        }
    }

    // ---------------------------------------------------------------------------------------
    // splitAt
    // ---------------------------------------------------------------------------------------

    @Nested
    class SplitAt {

        @Test
        void atStartMovesEverything() {
            FormattedText t = new FormattedText("abc");
            assertEquals("abc", t.splitAt(0).plainText());
            assertTrue(t.isEmpty());
        }

        @Test
        void inMiddle() {
            FormattedText t = new FormattedText("abcdef");
            assertEquals("def", t.splitAt(3).plainText());
            assertEquals("abc", t.plainText());
        }

        @Test
        void atEndGivesEmptyTail() {
            FormattedText t = new FormattedText("abc");
            assertTrue(t.splitAt(3).isEmpty());
            assertEquals("abc", t.plainText());
        }

        @Test
        void onEmptyText() {
            FormattedText t = new FormattedText("");
            assertTrue(t.splitAt(0).isEmpty());
            assertTrue(t.isEmpty());
        }

        @Test
        void insideRunKeepsFormattingOnBothHalves() {
            FormattedText t = mixed();
            FormattedText tail = t.splitAt(7); // inside "big"
            assertEquals(List.of(new TextRun("hello ", PLAIN), new TextRun("b", BOLD)),
                    t.runs());
            assertEquals(List.of(new TextRun("ig", BOLD), new TextRun(" world", ITALIC)),
                    tail.runs());
        }

        @Test
        void badOffsetRejected() {
            FormattedText t = new FormattedText("abc");
            assertBad(() -> t.splitAt(4));
            assertBad(() -> t.splitAt(-1));
            assertEquals("abc", t.plainText());
        }
    }

    // ---------------------------------------------------------------------------------------
    // append
    // ---------------------------------------------------------------------------------------

    @Nested
    class Append {

        @Test
        void toNonEmpty() {
            FormattedText t = new FormattedText("abc");
            t.append(new FormattedText("def", BOLD));
            assertEquals("abcdef", t.plainText());
            assertEquals(List.of(new TextRun("abc", PLAIN), new TextRun("def", BOLD)),
                    t.runs());
        }

        @Test
        void toEmpty() {
            FormattedText t = new FormattedText("");
            t.append(mixed());
            assertEquals(mixed(), t);
        }

        @Test
        void emptyToNonEmpty() {
            FormattedText t = mixed();
            t.append(new FormattedText(""));
            assertEquals(mixed(), t);
        }

        @Test
        void equalFormatsAtJoinMerge() {
            FormattedText t = new FormattedText("ab", BOLD);
            t.append(new FormattedText("cd", BOLD));
            assertEquals(List.of(new TextRun("abcd", BOLD)), t.runs());
        }

        @Test
        void argumentIsNotChanged() {
            FormattedText other = mixed();
            FormattedText t = new FormattedText("x");
            t.append(other);
            t.insert(t.length(), "y");
            assertEquals(mixed(), other);
        }

        @Test
        void splitThenAppendRestoresOriginal() {
            FormattedText t = mixed();
            t.append(t.splitAt(7));
            assertEquals(mixed(), t);
        }
    }

    // ---------------------------------------------------------------------------------------
    // applyFormat
    // ---------------------------------------------------------------------------------------

    @Nested
    class ApplyFormat {

        @Test
        void insideOneRunSplitsItInThree() {
            FormattedText t = new FormattedText("abcdef");
            t.applyFormat(2, 4, FormatChange.bold(true));
            assertEquals(List.of(new TextRun("ab", PLAIN), new TextRun("cd", BOLD),
                    new TextRun("ef", PLAIN)), t.runs());
        }

        @Test
        void acrossSeveralRunsChangesOnlyThatAttribute() {
            FormattedText t = mixed();
            t.applyFormat(3, 12, FormatChange.size(20));
            assertEquals(List.of(
                    new TextRun("hel", PLAIN),
                    new TextRun("lo ", PLAIN.with(FormatChange.size(20))),
                    new TextRun("big", BOLD.with(FormatChange.size(20))),
                    new TextRun(" wo", ITALIC.with(FormatChange.size(20))),
                    new TextRun("rld", ITALIC)), t.runs());
            assertNormalized(t);
        }

        @Test
        void charactersNeverChange() {
            FormattedText t = mixed();
            t.applyFormat(0, t.length(), FormatChange.color("red"));
            assertEquals("hello big world", t.plainText());
        }

        @Test
        void equalNeighboursMerge() {
            FormattedText t = mixed();
            t.applyFormat(6, 9, FormatChange.bold(false)); // "big" back to plain
            assertEquals(List.of(new TextRun("hello big", PLAIN),
                    new TextRun(" world", ITALIC)), t.runs());
        }

        @Test
        void wholeTextBecomesOneRun() {
            FormattedText t = mixed();
            t.applyFormat(0, t.length(), FormatChange.bold(false));
            t.applyFormat(0, t.length(), FormatChange.italic(false));
            assertEquals(List.of(new TextRun("hello big world", PLAIN)), t.runs());
        }

        @Test
        void noEmptyRunsAtBoundaries() {
            FormattedText t = mixed();
            t.applyFormat(0, 6, FormatChange.italic(true)); // exactly the first run
            assertNormalized(t);
            t.applyFormat(15, 15, FormatChange.bold(true)); // empty range at end
            assertNormalized(t);
        }

        @Test
        void returnsOldRunsForUndo() {
            FormattedText t = mixed();
            List<TextRun> old = t.applyFormat(2, 13, FormatChange.font("Arial"));
            assertNotEquals(mixed(), t);
            t.restoreRuns(old);
            assertEquals(mixed(), t);
        }

        @Test
        void onEmptyText() {
            FormattedText t = new FormattedText("");
            assertTrue(t.applyFormat(0, 0, FormatChange.bold(true)).isEmpty());
            assertTrue(t.isEmpty());
        }

        @Test
        void badRangeRejectedAndTextUnchanged() {
            FormattedText t = mixed();
            assertBad(() -> t.applyFormat(0, 16, FormatChange.bold(true)));
            assertBad(() -> t.applyFormat(5, 4, FormatChange.bold(true)));
            assertBad(() -> t.applyFormat(-1, 2, FormatChange.bold(true)));
            assertEquals(mixed(), t);
        }
    }

    // ---------------------------------------------------------------------------------------
    // equals
    // ---------------------------------------------------------------------------------------

    @Test
    void equalsComparesFormattingToo() {
        assertEquals(new FormattedText("abc"), new FormattedText("abc"));
        assertEquals(new FormattedText("abc").hashCode(), new FormattedText("abc").hashCode());
        assertNotEquals(new FormattedText("abc"), new FormattedText("abc", BOLD));
        assertEquals("abc", new FormattedText("abc", BOLD).toString());
    }
}
