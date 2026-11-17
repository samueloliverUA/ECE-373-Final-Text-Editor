package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MarkupRenderer}: each formatting attribute shows its marker, markers nest and
 * count toward the width, and the plain view of the same document has no markers.
 *
 * <p>Satisfies 6.6.2.
 */
class MarkupRendererTest {

    private final Renderer markup = new MarkupRenderer();
    private final Renderer plain = new PlainTextRenderer();
    private final LineBreaker greedy = new GreedyLineBreaker();

    /** A document with one paragraph "one two three", with {@code change} on "two". */
    private static Document withMiddleWord(FormatChange change) {
        Paragraph p = new Paragraph(new FormattedText("one two three"));
        p.text().applyFormat(4, 7, change);
        Document d = new Document();
        d.root().add(p);
        return d;
    }

    private String render(FormatChange change) {
        return markup.render(withMiddleWord(change), greedy, 80);
    }

    @Test
    void bold() {
        assertEquals("one *two* three", render(FormatChange.bold(true)));
    }

    @Test
    void italic() {
        assertEquals("one _two_ three", render(FormatChange.italic(true)));
    }

    @Test
    void size() {
        assertEquals("one [size=14: two] three", render(FormatChange.size(14)));
    }

    @Test
    void font() {
        assertEquals("one [font=Arial: two] three", render(FormatChange.font("Arial")));
    }

    @Test
    void color() {
        assertEquals("one [color=red: two] three", render(FormatChange.color("red")));
        assertEquals("one [color=#ff0000: two] three", render(FormatChange.color("#FF0000")));
    }

    @Test
    void defaultFormattingHasNoMarkers() {
        assertEquals("one two three", render(FormatChange.bold(false)));
        assertEquals("one two three", render(FormatChange.size(12)));
    }

    @Test
    void markersNestInDocumentedOrder() {
        Document d = withMiddleWord(FormatChange.bold(true));
        FormattedText t = d.paragraph(1).text();
        t.applyFormat(4, 7, FormatChange.italic(true));
        t.applyFormat(4, 7, FormatChange.size(20));
        t.applyFormat(4, 7, FormatChange.font("Arial"));
        t.applyFormat(4, 7, FormatChange.color("blue"));
        assertEquals("one [color=blue: [font=Arial: [size=20: _*two*_]]] three",
                markup.render(d, greedy, 80));
    }

    @Test
    void eachWordOfAPhraseIsMarked() {
        Paragraph p = new Paragraph(new FormattedText("a big bold phrase"));
        p.text().applyFormat(2, 17, FormatChange.bold(true));
        Document d = new Document();
        d.root().add(p);
        assertEquals("a *big* *bold* *phrase*", markup.render(d, greedy, 80));
    }

    @Test
    void formattingChangeInsideAWordIsMarkedPieceByPiece() {
        Paragraph p = new Paragraph(new FormattedText("bold"));
        p.text().applyFormat(0, 2, FormatChange.bold(true));
        Document d = new Document();
        d.root().add(p);
        assertEquals("*bo*ld", markup.render(d, greedy, 80));
    }

    @Test
    void titleFormattingIsShown() {
        Section s = new Section(new FormattedText("Intro"));
        s.title().applyFormat(0, 5, FormatChange.size(18));
        Document d = new Document();
        d.root().add(s);
        assertEquals("1 [size=18: Intro]", markup.render(d, greedy, 80));
    }

    @Test
    void markersCountTowardWidth() {
        // "one *two* three" is 15 wide: fits at 15, wraps at 14 even though the plain text
        // "one two three" (13) would fit.
        Document d = withMiddleWord(FormatChange.bold(true));
        assertEquals("one *two* three", markup.render(d, greedy, 15));
        assertEquals("one *two*\nthree", markup.render(d, greedy, 14));
        assertEquals("one two three", plain.render(d, greedy, 14));
    }

    @Test
    void plainViewOfSameDocumentHasNoMarkers() {
        Document d = withMiddleWord(FormatChange.bold(true));
        FormattedText t = d.paragraph(1).text();
        t.applyFormat(0, 13, FormatChange.italic(true));
        t.applyFormat(0, 3, FormatChange.size(30));
        t.applyFormat(8, 13, FormatChange.color("red"));
        t.applyFormat(8, 13, FormatChange.font("Courier"));
        String out = plain.render(d, greedy, 80);
        assertEquals("one two three", out);
        for (String marker : new String[] {"*", "_", "[", "]", "="}) {
            assertFalse(out.contains(marker), marker);
        }
        assertTrue(markup.render(d, greedy, 80).contains("["));
    }

    @Test
    void emptyDocument() {
        assertEquals("(empty document)", markup.render(new Document(), greedy, 80));
    }
}
