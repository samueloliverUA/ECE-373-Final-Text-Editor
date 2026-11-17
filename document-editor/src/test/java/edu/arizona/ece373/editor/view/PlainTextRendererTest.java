package edu.arizona.ece373.editor.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.FormatChange;
import edu.arizona.ece373.editor.model.FormattedText;
import edu.arizona.ece373.editor.model.Paragraph;
import edu.arizona.ece373.editor.model.Section;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PlainTextRenderer} and {@link AbstractRenderer}: heading numbering, wrapping
 * within the width, blank lines between blocks, and the empty document.
 *
 * <p>Satisfies 6.6.1, 6.2.3.
 */
class PlainTextRendererTest {

    private final Renderer plain = new PlainTextRenderer();
    private final LineBreaker greedy = new GreedyLineBreaker();

    private static Paragraph para(String text) {
        return new Paragraph(new FormattedText(text));
    }

    private static Section section(String title) {
        return new Section(new FormattedText(title));
    }

    /** 1 Intro { p; 1.1 Background { p }; 1.2 Scope }; 2 Methods { p }. */
    private static Document sample() {
        Document d = new Document();
        Section intro = section("Intro");
        Section background = section("Background");
        background.add(para("Some history."));
        intro.add(para("Opening words."));
        intro.add(background);
        intro.add(section("Scope"));
        Section methods = section("Methods");
        methods.add(para("How it was done."));
        d.root().add(intro);
        d.root().add(methods);
        return d;
    }

    @Test
    void emptyDocument() {
        assertEquals("(empty document)", plain.render(new Document(), greedy, 80));
    }

    @Test
    void headingNumbering() {
        assertEquals(String.join("\n",
                "1 Intro",
                "",
                "Opening words.",
                "",
                "1.1 Background",
                "",
                "Some history.",
                "",
                "1.2 Scope",
                "",
                "2 Methods",
                "",
                "How it was done."), plain.render(sample(), greedy, 80));
    }

    @Test
    void numberingRestartsUnderEachParentAndGoesDeep() {
        Document d = new Document();
        Section a = section("A");
        Section b = section("B");
        Section b1 = section("B1");
        Section b11 = section("B11");
        b1.add(b11);
        b.add(b1);
        b.add(section("B2"));
        a.add(section("A1"));
        d.root().add(a);
        d.root().add(b);
        String out = plain.render(d, greedy, 80);
        assertEquals("1 A\n\n1.1 A1\n\n2 B\n\n2.1 B1\n\n2.1.1 B11\n\n2.2 B2", out);
    }

    @Test
    void paragraphsBeforeAnySectionAreUnnumbered() {
        Document d = new Document();
        d.root().add(para("Preface."));
        d.root().add(section("One"));
        assertEquals("Preface.\n\n1 One", plain.render(d, greedy, 80));
    }

    /** Lines may only exceed the width when they are a single over-long word. */
    @Test
    void noLineWiderThanWidth() {
        Document d = new Document();
        Section s = section("A rather long section title that must wrap too");
        s.add(para("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do "
                + "eiusmod tempor incididunt ut labore et dolore magna aliqua."));
        d.root().add(s);
        for (int width = 10; width <= 60; width += 5) {
            for (String line : plain.render(d, greedy, width).split("\n")) {
                assertTrue(line.length() <= width || !line.contains(" "),
                        "'" + line + "' wider than " + width);
            }
        }
    }

    @Test
    void wrapsAtWidth() {
        Document d = new Document();
        d.root().add(para("aaa bb cc ddddd"));
        assertEquals("aaa bb\ncc\nddddd", plain.render(d, greedy, 6));
    }

    @Test
    void extraSpacesCollapse() {
        Document d = new Document();
        d.root().add(para("  two   spaces  "));
        assertEquals("two spaces", plain.render(d, greedy, 80));
    }

    @Test
    void emptyParagraphIsOneEmptyLine() {
        Document d = new Document();
        d.root().add(para("a"));
        d.root().add(para(""));
        d.root().add(para("b"));
        assertEquals("a\n\n\n\nb", plain.render(d, greedy, 80));
    }

    @Test
    void formattingIsNotShown() {
        Document d = new Document();
        Paragraph p = para("bold words");
        p.text().applyFormat(0, 4, FormatChange.bold(true));
        p.text().applyFormat(5, 10, FormatChange.size(30));
        d.root().add(p);
        assertEquals("bold words", plain.render(d, greedy, 80));
    }

    @Test
    void renderingTwiceGivesSameOutput() {
        Document d = sample();
        assertEquals(plain.render(d, greedy, 30), plain.render(d, greedy, 30));
    }

    @Test
    void renderingDoesNotChangeDocument() {
        Document d = sample();
        String outline = d.outline();
        plain.render(d, greedy, 12);
        assertEquals(outline, d.outline());
    }
}
