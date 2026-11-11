package edu.arizona.ece373.editor.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.app.Action;
import edu.arizona.ece373.editor.model.TextRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link CommandParser} and {@link Tokens}.
 *
 * <p>Satisfies requirements 6.1.1 (text command interface), 6.1.3 (command coverage) and 6.1.5
 * (input validation). Command syntax follows Section 1.3 of the design document:
 * positions are {@code p<n>:<offset>}, ranges are {@code p<n>:<start>-<end>} (end excluded),
 * section paths are dotted ({@code 2.1}) and text arguments are quoted.
 *
 * <p>The parser never touches a Document, so these tests only check that a line produces an
 * {@link Action} (or a {@link ParseException}); they do not run the action.
 *
 * <p>Covers every verb in the Section 1.3 table, including verbs for features that land in
 * later weeks; those cases fail until the verb is registered in {@link CommandParser}.
 */
class CommandParserTest {

    private CommandParser parser;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /** Asserts that {@code line} parses to a non-null Action. */
    private void assertParses(String line) {
        Action action = assertDoesNotThrow(() -> parser.parse(line),
                () -> "expected '" + line + "' to parse");
        assertNotNull(action, () -> "parse('" + line + "') returned null");
    }

    /**
     * Asserts that {@code line} is rejected with a one-line, non-empty message that mentions
     * {@code fragment} (case-insensitive), so the user can tell what was wrong.
     */
    private void assertParseError(String line, String fragment) {
        ParseException e = assertThrows(ParseException.class, () -> parser.parse(line),
                () -> "expected '" + line + "' to be rejected");
        assertUsefulMessage(e, fragment);
    }

    private static void assertUsefulMessage(ParseException e, String fragment) {
        String msg = e.getMessage();
        assertNotNull(msg, "ParseException must have a message");
        assertFalse(msg.isBlank(), "ParseException message must not be blank");
        assertFalse(msg.contains("\n"), () -> "message must be one line: " + msg);
        assertTrue(msg.toLowerCase().contains(fragment.toLowerCase()),
                () -> "message '" + msg + "' should mention '" + fragment + "'");
    }

    // ---------------------------------------------------------------------------------------
    // 1. Every verb in the Section 1.3 table parses to an Action
    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Structure and history verbs (week of Oct 28)")
    class Week1Verbs {

        @ParameterizedTest
        @ValueSource(strings = {
            "section add \"Introduction\"",
            "section add 2 \"Setup\"",
            "section add 2.1.3 \"Deep subsection\"",
            "para add 2 \"Some text\"",
            "para add 2.1 \"Some text in a subsection\"",
            "outline",
            "undo",
            "redo",
            "quit",
        })
        void parses(String line) {
            assertParses(line);
        }

        @Test
        void surroundingWhitespaceIsIgnored() {
            assertParses("   undo   ");
            assertParses("\tsection   add    \"Intro\"  ");
        }
    }

    @Nested
    @DisplayName("Editing verbs")
    class EditingVerbs {

        @ParameterizedTest
        @ValueSource(strings = {
            "insert p2:14 \"text\"",
            "insert p1:0 \"at the start\"",
            "delete p2:5-12",
            "replace p2:5-12 \"new text\"",
            "move p2:40-60 p2:0",
            "split p2:30",
            "merge p2",
        })
        void parses(String line) {
            assertParses(line);
        }
    }

    @Nested
    @DisplayName("Formatting verbs")
    class FormattingVerbs {

        @ParameterizedTest
        @ValueSource(strings = {
            "format p2:4-25 bold on",
            "format p2:4-25 bold off",
            "format p2:4-25 italic on",
            "format p2:4-25 font \"Arial\"",
            "format p2:4-25 size 14",
            "format p2:4-25 color red",
            "format p2:4-25 color #FF0000",
            "format title 1 size 18",
            "format title 2.1 bold on",
        })
        void parses(String line) {
            assertParses(line);
        }

        @Test
        void sizeMustBeANumber() {
            assertParseError("format p2:4-25 size big", "big");
        }
    }

    @Nested
    @DisplayName("Checkpoint and viewing verbs")
    class CheckpointAndViewVerbs {

        @ParameterizedTest
        @ValueSource(strings = {
            "checkpoint save before-results-edit",
            "checkpoint restore before-results-edit",
            "checkpoint list",
            "view plain",
            "view markup",
            "width 50",
            "breaker greedy",
            "breaker balanced",
            "show",
            "stats",
        })
        void parses(String line) {
            assertParses(line);
        }

        @Test
        void widthMustBeANumber() {
            assertParseError("width fifty", "fifty");
        }

        @Test
        void checkpointSaveNeedsAName() {
            assertParseError("checkpoint save", "missing");
        }
    }

    @Nested
    @DisplayName("Search and automation verbs")
    class SearchAndAutomationVerbs {

        @ParameterizedTest
        @ValueSource(strings = {
            "find \"program\"",
            "find \"program\" word",
            "replace-first \"old\" \"new\"",
            "replace-first \"old\" \"new\" word",
            "replace-all \"program\" \"system\"",
            "replace-all \"program\" \"system\" word",
            "macro record fmt-and-show",
            "macro end",
            "macro play fmt-and-show",
            "run demo.txt",
        })
        void parses(String line) {
            assertParses(line);
        }

        @Test
        void replaceAllNeedsTwoStrings() {
            assertParseError("replace-all \"program\"", "missing");
        }
    }

    // ---------------------------------------------------------------------------------------
    // 2. Tokens: quoted strings, positions, ranges, section paths
    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Tokens")
    class TokensTests {

        @Test
        void quotedStringKeepsItsSpaces() throws ParseException {
            Tokens t = Tokens.parse("2.1 \"two   words here\"");
            assertEquals(2, t.count());
            assertEquals("two   words here", t.text(1));
        }

        @Test
        void emptyQuotedStringIsAllowedByTheTokenizer() throws ParseException {
            // An empty title is rejected later by the command (UC1 alternate flow 1b), not here.
            Tokens t = Tokens.parse("\"\"");
            assertEquals(1, t.count());
            assertEquals("", t.text(0));
        }

        @Test
        void severalQuotedStrings() throws ParseException {
            Tokens t = Tokens.parse("\"old text\" \"new text\" word");
            assertEquals(3, t.count());
            assertEquals("old text", t.text(0));
            assertEquals("new text", t.text(1));
            assertTrue(t.hasFlag("word"));
        }

        @Test
        void unterminatedQuoteIsRejected() {
            ParseException e = assertThrows(ParseException.class,
                    () -> Tokens.parse("\"no closing quote"));
            assertUsefulMessage(e, "quote");
        }

        @Test
        void textMustBeQuoted() throws ParseException {
            Tokens t = Tokens.parse("unquoted");
            ParseException e = assertThrows(ParseException.class, () -> t.text(0));
            assertUsefulMessage(e, "unquoted");
        }

        @Test
        void hasFlagIsFalseWhenAbsent() throws ParseException {
            assertFalse(Tokens.parse("\"program\"").hasFlag("word"));
        }

        @Test
        void quotedWordIsNotAFlag() throws ParseException {
            assertFalse(Tokens.parse("\"word\"").hasFlag("word"));
        }

        @Test
        void number() throws ParseException {
            assertEquals(50, Tokens.parse("50").number(0));
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "12abc", "1.5", "\"14\""})
        void nonNumberIsRejected(String token) throws ParseException {
            Tokens t = Tokens.parse(token);
            ParseException e = assertThrows(ParseException.class, () -> t.number(0));
            assertUsefulMessage(e, token.replace("\"", ""));
        }

        @Test
        void range() throws ParseException {
            TextRange r = Tokens.parse("p2:5-12").range(0);
            assertEquals(2, r.paragraph);
            assertEquals(5, r.start);
            assertEquals(12, r.end);
        }

        @Test
        void rangeStartingAtZero() throws ParseException {
            TextRange r = Tokens.parse("p1:0-3").range(0);
            assertEquals(1, r.paragraph);
            assertEquals(0, r.start);
            assertEquals(3, r.end);
        }

        @Test
        void rangeWithMultiDigitParts() throws ParseException {
            TextRange r = Tokens.parse("p12:100-2048").range(0);
            assertEquals(12, r.paragraph);
            assertEquals(100, r.start);
            assertEquals(2048, r.end);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "p2:9-3",   // start after end
            "p0:1-2",   // paragraphs are numbered from 1
            "p2:5",     // a position, not a range
            "p2:-1-3",  // negative start
            "p2:a-3",   // non-number
            "2:5-12",   // missing 'p'
            "p:5-12",   // missing paragraph number
            "p2:5-",    // missing end
            "p2-5-12",  // wrong separator
        })
        void badRangeIsRejected(String token) throws ParseException {
            Tokens t = Tokens.parse(token);
            ParseException e = assertThrows(ParseException.class, () -> t.range(0));
            assertUsefulMessage(e, token);
        }

        @ParameterizedTest
        @ValueSource(strings = {"p2:14", "p1:0", "p10:250"})
        void position(String token) throws ParseException {
            Tokens t = Tokens.parse(token);
            assertDoesNotThrow(() -> t.position(0));
        }

        @ParameterizedTest
        @ValueSource(strings = {"p0:3", "p2:-1", "p2:x", "p2", "2:14", "p2:5-12"})
        void badPositionIsRejected(String token) throws ParseException {
            Tokens t = Tokens.parse(token);
            ParseException e = assertThrows(ParseException.class, () -> t.position(0));
            assertUsefulMessage(e, token);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2", "2.1", "1.2.3.4.5"})
        void sectionPath(String token) throws ParseException {
            assertEquals(token, Tokens.parse(token).sectionPath(0).toString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "2.0", "2.", ".1", "2..1", "a.1", "-1"})
        void badSectionPathIsRejected(String token) throws ParseException {
            Tokens t = Tokens.parse(token);
            ParseException e = assertThrows(ParseException.class, () -> t.sectionPath(0));
            assertUsefulMessage(e, token);
        }

        @Test
        void accessorPastTheEndIsAMissingArgument() throws ParseException {
            Tokens t = Tokens.parse("2.1");
            assertEquals(1, t.count());
            assertUsefulMessage(assertThrows(ParseException.class, () -> t.text(1)), "missing");
            assertUsefulMessage(assertThrows(ParseException.class, () -> t.number(1)), "missing");
            assertUsefulMessage(assertThrows(ParseException.class, () -> t.range(1)), "missing");
            assertUsefulMessage(
                    assertThrows(ParseException.class, () -> t.position(1)), "missing");
            assertUsefulMessage(
                    assertThrows(ParseException.class, () -> t.sectionPath(1)), "missing");
        }

        @Test
        void emptyArgumentList() throws ParseException {
            Tokens t = Tokens.parse("");
            assertEquals(0, t.count());
            assertFalse(t.hasFlag("word"));
        }
    }

    // ---------------------------------------------------------------------------------------
    // 3. Malformed lines -> ParseException with a useful message
    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("Malformed lines")
    class MalformedLines {

        @Test
        void unknownVerbIsNamed() {
            assertParseError("frobnicate p2:3", "frobnicate");
        }

        @Test
        void unknownSecondWordOfTwoWordVerb() {
            assertParseError("section remove 2", "remove");
        }

        @Test
        void firstWordOfTwoWordVerbAlone() {
            assertParseError("section", "section");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t"})
        void blankLineIsRejected(String line) {
            assertThrows(ParseException.class, () -> parser.parse(line));
        }

        @Test
        void sectionAddWithoutTitle() {
            assertParseError("section add", "missing");
        }

        @Test
        void sectionAddWithParentButNoTitle() {
            assertParseError("section add 2", "missing");
        }

        @Test
        void paraAddWithoutText() {
            assertParseError("para add 2", "missing");
        }

        @Test
        void paraAddWithoutSection() {
            assertParseError("para add", "missing");
        }

        @Test
        void paraAddWithNonNumberSection() {
            assertParseError("para add abc \"text\"", "abc");
        }

        @Test
        void paraAddWithZeroSection() {
            assertParseError("para add 0 \"text\"", "0");
        }

        @Test
        void paraAddWithUnquotedText() {
            assertParseError("para add 2 hello", "hello");
        }

        @Test
        void unterminatedQuoteOnALine() {
            assertParseError("section add \"Intro", "quote");
        }
    }

    @Nested
    @DisplayName("Malformed editing lines")
    class MalformedEditingLines {

        @Test
        void startAfterEnd() {
            assertParseError("delete p2:9-3", "p2:9-3");
        }

        @Test
        void paragraphZeroInRange() {
            assertParseError("delete p0:1-3", "p0:1-3");
        }

        @Test
        void paragraphZeroInPosition() {
            assertParseError("insert p0:3 \"x\"", "p0:3");
        }

        @Test
        void mergeParagraphZero() {
            assertParseError("merge p0", "p0");
        }

        @Test
        void nonNumberOffset() {
            assertParseError("split p2:abc", "p2:abc");
        }

        @Test
        void insertWithoutText() {
            assertParseError("insert p2:14", "missing");
        }

        @Test
        void moveWithoutDestination() {
            assertParseError("move p2:40-60", "missing");
        }

        @Test
        void deleteNeedsARangeNotAPosition() {
            assertParseError("delete p2:5", "p2:5");
        }
    }
}
