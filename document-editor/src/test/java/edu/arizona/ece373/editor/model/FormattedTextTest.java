package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link FormattedText} (6.3.x).
 *
 * <p>Covers insert, cut, splitAt and append at the start, middle and end and on
 * empty text, and bad offsets throwing {@link EditException}.
 *
 * <p>Not yet covered: formatting kept on inserted and remaining characters, and
 * applyFormat (6.5.2, 6.5.3). Those tests are added with the {@code TextRun}
 * version of FormattedText.
 */
class FormattedTextTest {

    //constructor, length, copy

    @Test
    void holdsTheTextItWasGiven() {
        FormattedText t = new FormattedText("Hello world");

        assertEquals("Hello world", t.plainText());
        assertEquals(11, t.length());
        assertEquals("Hello world", t.toString());
    }

    @Test
    void nullBecomesEmptyText() {
        FormattedText t = new FormattedText(null);

        assertEquals("", t.plainText());
        assertEquals(0, t.length());
    }

    @Test
    void copyIsIndependent() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText c = t.copy();
        c.cut(0, 6);

        assertEquals("Hello world", t.plainText());
        assertEquals("world", c.plainText());
    }

    // cut / insert / append / splitAt (plain-text version).
    // All use "Hello world" (11 characters): "Hello" is 0-5, " " is 5-6, "world" is 6-11.

    //cut

    @Test
    void cutFromTheStart() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(0, 6);

        assertEquals("world", t.plainText());
        assertEquals("Hello ", removed.plainText());
    }

    @Test
    void cutFromTheMiddle() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(5, 6);

        assertEquals("Helloworld", t.plainText());
        assertEquals(" ", removed.plainText());
    }

    @Test
    void cutToTheEnd() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(5, 11);

        assertEquals("Hello", t.plainText());
        assertEquals(" world", removed.plainText());
    }

    @Test
    void cutTheWholeTextLeavesItEmpty() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(0, 11);

        assertEquals("", t.plainText());
        assertEquals(0, t.length());
        assertEquals("Hello world", removed.plainText());
    }

    @Test
    void cutAnEmptyRangeChangesNothing() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(3, 3);

        assertEquals("Hello world", t.plainText());
        assertEquals("", removed.plainText());
    }

    @Test
    void cutRejectsBadRanges() {
        FormattedText t = new FormattedText("Hello world");

        assertThrows(EditException.class, () -> t.cut(-1, 3));
        assertThrows(EditException.class, () -> t.cut(0, 12));
        assertThrows(EditException.class, () -> t.cut(6, 5));
        assertEquals("Hello world", t.plainText());
    }

    //insert

    @Test
    void insertAtTheStart() {
        FormattedText t = new FormattedText("Hello world");

        t.insert(0, new FormattedText("Oh, "));

        assertEquals("Oh, Hello world", t.plainText());
    }

    @Test
    void insertInTheMiddle() {
        FormattedText t = new FormattedText("Hello world");

        t.insert(5, new FormattedText(" brave"));

        assertEquals("Hello brave world", t.plainText());
    }

    @Test
    void insertAtTheEnd() {
        FormattedText t = new FormattedText("Hello world");

        t.insert(11, new FormattedText("!"));

        assertEquals("Hello world!", t.plainText());
    }

    @Test
    void insertIntoEmptyText() {
        FormattedText t = new FormattedText("");

        t.insert(0, new FormattedText("first"));

        assertEquals("first", t.plainText());
    }

    @Test
    void insertAnEmptyPieceChangesNothing() {
        FormattedText t = new FormattedText("Hello world");

        t.insert(5, new FormattedText(""));

        assertEquals("Hello world", t.plainText());
    }

    @Test
    void insertDoesNotChangeThePiece() {
        FormattedText t = new FormattedText("Hello world");
        FormattedText piece = new FormattedText(" brave");

        t.insert(5, piece);

        assertEquals(" brave", piece.plainText());
    }

    @Test
    void insertRejectsBadOffsets() {
        FormattedText t = new FormattedText("Hello world");

        assertThrows(EditException.class, () -> t.insert(-1, new FormattedText("x")));
        assertThrows(EditException.class, () -> t.insert(12, new FormattedText("x")));
        assertEquals("Hello world", t.plainText());
    }

    @Test
    void cutThenInsertPutsTheTextBack() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText removed = t.cut(5, 11);
        t.insert(5, removed);

        assertEquals("Hello world", t.plainText());
    }

    //append

    @Test
    void appendAddsToTheEnd() {
        FormattedText t = new FormattedText("Hello");

        t.append(new FormattedText(" world"));

        assertEquals("Hello world", t.plainText());
    }

    @Test
    void appendToEmptyText() {
        FormattedText t = new FormattedText("");

        t.append(new FormattedText("world"));

        assertEquals("world", t.plainText());
    }

    @Test
    void appendAnEmptyPieceChangesNothing() {
        FormattedText t = new FormattedText("Hello");

        t.append(new FormattedText(""));

        assertEquals("Hello", t.plainText());
    }

    @Test
    void appendDoesNotChangeThePiece() {
        FormattedText t = new FormattedText("Hello");
        FormattedText piece = new FormattedText(" world");

        t.append(piece);
        t.cut(0, 5);

        assertEquals(" world", piece.plainText());
    }

    //splitAt

    @Test
    void splitAtTheStartMovesEverything() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText right = t.splitAt(0);

        assertEquals("", t.plainText());
        assertEquals("Hello world", right.plainText());
    }

    @Test
    void splitInTheMiddle() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText right = t.splitAt(6);

        assertEquals("Hello ", t.plainText());
        assertEquals("world", right.plainText());
    }

    @Test
    void splitAtTheEndReturnsAnEmptyPiece() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText right = t.splitAt(11);

        assertEquals("Hello world", t.plainText());
        assertEquals("", right.plainText());
    }

    @Test
    void splitAtRejectsBadOffsets() {
        FormattedText t = new FormattedText("Hello world");

        assertThrows(EditException.class, () -> t.splitAt(-1));
        assertThrows(EditException.class, () -> t.splitAt(12));
        assertEquals("Hello world", t.plainText());
    }

    @Test
    void splitThenAppendPutsTheTextBack() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText right = t.splitAt(5);
        t.append(right);

        assertEquals("Hello world", t.plainText());
    }

    @Test
    void splitPieceIsIndependent() {
        FormattedText t = new FormattedText("Hello world");

        FormattedText right = t.splitAt(5);
        right.cut(0, 1);

        assertEquals("Hello", t.plainText());
        assertEquals("world", right.plainText());
    }
}
