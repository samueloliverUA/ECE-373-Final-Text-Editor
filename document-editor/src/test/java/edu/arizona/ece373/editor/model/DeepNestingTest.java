package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.app.Editor;
import edu.arizona.ece373.editor.app.Result;
import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import edu.arizona.ece373.editor.history.DeleteTextCommand;
import edu.arizona.ece373.editor.history.FormatTextCommand;
import edu.arizona.ece373.editor.history.FormatTitleCommand;
import edu.arizona.ece373.editor.history.InsertTextCommand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests operations on deeply nested content: a document {@value #DEPTH} levels deep, where
 * lookup, the outline, rendering (plain and markup, both breakers), statistics, find and
 * replace, formatting, editing, undo and checkpoint restore all reach the deepest paragraph.
 *
 * <p>Satisfies 6.2.3.
 */
class DeepNestingTest {

    private static final int DEPTH = 12;
    private static final String DEEP_TEXT = "deepest words live here";

    private Editor editor;
    private SectionPath deepPath;

    /** Sections L1 &gt; L2 &gt; ... &gt; L12; p1 "top" in L1, p2 {@value #DEEP_TEXT} in L12. */
    @BeforeEach
    void build() {
        editor = new Editor();
        SectionPath parent = null;
        for (int level = 1; level <= DEPTH; level++) {
            ok(editor.perform(new AddSectionCommand(parent, "L" + level)));
            parent = SectionPath.child(parent, 1);
            if (level == 1) {
                // Before L2 exists, so "top" comes first in document order.
                ok(editor.perform(new AddParagraphCommand(parent, "top")));
            }
        }
        deepPath = parent;
        ok(editor.perform(new AddParagraphCommand(deepPath, DEEP_TEXT)));
    }

    private static void ok(Result r) {
        assertTrue(r.ok, r.message);
    }

    private Document doc() {
        return editor.document();
    }

    private Paragraph deepest() {
        return doc().paragraph(2);
    }

    private static String ones(int n) {
        return String.join(".", java.util.Collections.nCopies(n, "1"));
    }

    @Test
    void pathReachesDeepestSection() {
        assertEquals(DEPTH, deepPath.depth());
        assertEquals(ones(DEPTH), deepPath.toString());
        assertEquals("L" + DEPTH, doc().section(deepPath).title().plainText());
    }

    @Test
    void oneLevelTooDeepIsAnError() {
        Result r = editor.perform(new AddParagraphCommand(SectionPath.child(deepPath, 1), "x"));
        assertFalse(r.ok);
        assertTrue(r.message.contains("No section"), r.message);
    }

    @Test
    void deepestParagraphIsNumberedInDocumentOrder() {
        assertEquals(2, doc().paragraphs().size());
        assertEquals("top", doc().paragraph(1).text().plainText());
        assertEquals(DEEP_TEXT, deepest().text().plainText());
        assertEquals(2, doc().numberOf(deepest()));
    }

    @Test
    void outlineReachesDeepestLevel() {
        String outline = doc().outline();
        assertTrue(outline.contains("  ".repeat(DEPTH - 1) + ones(DEPTH) + " L" + DEPTH),
                outline);
        assertTrue(outline.contains("p2: \"" + DEEP_TEXT + "\""), outline);
    }

    @Test
    void plainRenderReachesDeepestHeadingAndParagraph() {
        List<String> lines = List.of(editor.render().split("\n"));
        assertTrue(lines.contains(ones(DEPTH) + " L" + DEPTH), lines.toString());
        assertEquals(DEEP_TEXT, lines.get(lines.size() - 1));
        assertTrue(lines.contains(ones(DEPTH - 1) + " L" + (DEPTH - 1)));
    }

    @Test
    void narrowRenderWithBothBreakersKeepsEveryWord() {
        ok(editor.setWidth(10));
        for (String breaker : new String[] {"greedy", "balanced"}) {
            ok(editor.setBreaker(breaker));
            String out = editor.render();
            for (String line : out.split("\n")) {
                // Heading numbers like 1.1.1.1... are longer than 10 and sit alone.
                assertTrue(line.length() <= 10 || !line.contains(" "), breaker + ": " + line);
            }
            for (String word : DEEP_TEXT.split(" ")) {
                assertTrue(out.contains(word), breaker + " lost " + word);
            }
        }
    }

    @Test
    void statisticsCountDeepestParagraph() {
        // "top" (1 word, 3 chars) + DEEP_TEXT (4 words, 23 chars); titles not counted
        assertEquals(new edu.arizona.ece373.editor.view.Statistics(5, 26, 2), editor.stats());
        ok(editor.perform(new DeleteTextCommand(new TextRange(2, 0, 8))));
        assertEquals(new edu.arizona.ece373.editor.view.Statistics(4, 18, 2), editor.stats());
    }

    @Test
    void findAndReplaceReachDeepestParagraph() {
        assertEquals("Found 1: p2:8-13",
                editor.find("words", new edu.arizona.ece373.editor.search.WholeWordMatch())
                        .message);
        ok(editor.replaceAll("live", "sleep",
                new edu.arizona.ece373.editor.search.SubstringMatch()));
        assertEquals("deepest words sleep here", deepest().text().plainText());
        ok(editor.undo());
        assertEquals(DEEP_TEXT, deepest().text().plainText());
    }

    @Test
    void formattingDeepestParagraphShowsInMarkup() {
        ok(editor.perform(new FormatTextCommand(new TextRange(2, 0, 7),
                FormatChange.bold(true))));
        ok(editor.setView("markup"));
        assertTrue(editor.render().endsWith("*deepest* words live here"), editor.render());
        ok(editor.setView("plain"));
        assertTrue(editor.render().endsWith(DEEP_TEXT));
    }

    @Test
    void formattingDeepestTitleShowsInMarkup() {
        ok(editor.perform(new FormatTitleCommand(deepPath, FormatChange.size(18))));
        ok(editor.setView("markup"));
        assertTrue(editor.render().contains(ones(DEPTH) + " [size=18: L" + DEPTH + "]"),
                editor.render());
    }

    @Test
    void editAndUndoAtDepth() {
        String before = editor.render();
        ok(editor.perform(new InsertTextCommand(2, 0, "the ")));
        assertEquals("the " + DEEP_TEXT, deepest().text().plainText());
        ok(editor.perform(new DeleteTextCommand(new TextRange(2, 0, 12))));
        assertEquals("words live here", deepest().text().plainText());
        ok(editor.undo());
        ok(editor.undo());
        assertEquals(before, editor.render());
    }

    @Test
    void checkpointRestoreBringsBackDeepContent() {
        String before = editor.render();
        ok(editor.saveCheckpoint("deep"));
        ok(editor.perform(new DeleteTextCommand(new TextRange(2, 0, DEEP_TEXT.length()))));
        ok(editor.perform(new AddParagraphCommand(deepPath, "extra")));
        assertFalse(before.equals(editor.render()));
        ok(editor.restoreCheckpoint("deep"));
        assertEquals(before, editor.render());
        assertEquals(DEEP_TEXT, deepest().text().plainText());
        ok(editor.undo());
        assertTrue(editor.render().endsWith("extra"));
    }

    @Test
    void deepCopyReachesDeepestParagraph() {
        Section copy = doc().root().deepCopy();
        Section s = copy;
        for (int level = 0; level < DEPTH; level++) {
            s = s.subsections().get(0);
        }
        assertEquals("L" + DEPTH, s.title().plainText());
        List<Paragraph> paras = new java.util.ArrayList<>();
        s.collectParagraphs(paras);
        assertEquals(DEEP_TEXT, paras.get(0).text().plainText());
    }

    @Test
    void fiftyLevelsDeepStillWorks() {
        Editor e = new Editor();
        SectionPath parent = null;
        for (int level = 1; level <= 50; level++) {
            ok(e.perform(new AddSectionCommand(parent, "S" + level)));
            parent = SectionPath.child(parent, 1);
        }
        ok(e.perform(new AddParagraphCommand(parent, "bottom")));
        assertTrue(e.render().endsWith("bottom"));
        assertTrue(e.document().outline().contains(ones(50) + " S50"));
    }
}
