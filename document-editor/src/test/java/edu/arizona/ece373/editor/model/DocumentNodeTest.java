package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DocumentNode#writeTo}: the order and levels in which {@link Section} and
 * {@link Paragraph} report themselves to a {@link DocumentWriter}, at any depth.
 *
 * <p>Satisfies 6.2.3.
 */
class DocumentNodeTest {

    /** Records every call as a line such as {@code H1 Intro} or {@code P hello}. */
    private static final class RecordingWriter implements DocumentWriter {
        final List<String> calls = new ArrayList<>();
        final List<FormattedText> texts = new ArrayList<>();

        @Override
        public void heading(FormattedText t, int level) {
            calls.add("H" + level + " " + t.plainText());
            texts.add(t);
        }

        @Override
        public void paragraph(FormattedText t) {
            calls.add("P " + t.plainText());
            texts.add(t);
        }
    }

    private static Paragraph para(String text) {
        return new Paragraph(new FormattedText(text));
    }

    private static Section section(String title) {
        return new Section(new FormattedText(title));
    }

    private static List<String> write(DocumentNode n, int depth) {
        RecordingWriter w = new RecordingWriter();
        n.writeTo(w, depth);
        return w.calls;
    }

    @Test
    void paragraphWritesItsTextOnly() {
        assertEquals(List.of("P hello"), write(para("hello"), 3));
    }

    @Test
    void emptyDocumentWritesOnlyTheRootHeading() {
        assertEquals(List.of("H0 "), write(new Document().root(), 0));
    }

    @Test
    void headingComesBeforeChildrenInDocumentOrder() {
        Section root = section("");
        Section intro = section("Intro");
        intro.add(para("a"));
        intro.add(para("b"));
        root.add(para("preface"));
        root.add(intro);
        root.add(section("End"));
        assertEquals(List.of("H0 ", "P preface", "H1 Intro", "P a", "P b", "H1 End"),
                write(root, 0));
    }

    @Test
    void paragraphsBetweenSubsectionsKeepTheirPlace() {
        Section s = section("S");
        Section sub = section("Sub");
        sub.add(para("inner"));
        s.add(para("before"));
        s.add(sub);
        s.add(para("after"));
        assertEquals(List.of("H1 S", "P before", "H2 Sub", "P inner", "P after"),
                write(s, 1));
    }

    @Test
    void levelsKeepIncreasingAtDepth() {
        Section root = section("");
        Section current = root;
        for (int i = 1; i <= 12; i++) {
            Section next = section("L" + i);
            current.add(next);
            current = next;
        }
        current.add(para("deepest"));

        List<String> calls = write(root, 0);
        assertEquals(14, calls.size());
        assertEquals("H12 L12", calls.get(12));
        assertEquals("P deepest", calls.get(13));
    }

    @Test
    void startingDepthIsRespected() {
        Section s = section("S");
        s.add(section("T"));
        assertEquals(List.of("H5 S", "H6 T"), write(s, 5));
    }

    @Test
    void writerGetsTheNodesOwnTextWithFormatting() {
        Paragraph p = para("bold");
        p.text().applyFormat(0, 4, FormatChange.bold(true));
        Section s = section("Title");
        s.add(p);
        RecordingWriter w = new RecordingWriter();
        s.writeTo(w, 1);
        assertSame(s.title(), w.texts.get(0));
        assertSame(p.text(), w.texts.get(1));
        assertEquals(true, w.texts.get(1).formatAt(0).bold);
    }

    @Test
    void writingDoesNotChangeTheTree() {
        Document d = new Document();
        Section s = section("Intro");
        s.add(para("x"));
        d.root().add(s);
        String before = d.outline();
        d.root().writeTo(new RecordingWriter(), 0);
        assertEquals(before, d.outline());
    }
}
