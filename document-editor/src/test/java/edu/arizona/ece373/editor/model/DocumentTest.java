package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.arizona.ece373.editor.history.AddParagraphCommand;
import edu.arizona.ece373.editor.history.AddSectionCommand;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link Document} and {@link Paragraph}: paragraph numbering in document order across
 * sections and subsections, section lookup by path, missing paragraphs and sections, the
 * outline, listener notification, and saving/restoring through {@link DocumentMemento}.
 *
 * <p>Satisfies 6.2.1.
 */
class DocumentTest {

    private static Paragraph para(String text) {
        return new Paragraph(new FormattedText(text));
    }

    private static Section section(String title) {
        return new Section(new FormattedText(title));
    }

    private static String message(Runnable r) {
        return assertThrows(EditException.class, r::run).getMessage();
    }

    // ---------------------------------------------------------------------------------------
    // Empty document
    // ---------------------------------------------------------------------------------------

    @Nested
    class EmptyDocument {

        private final Document doc = new Document();

        @Test
        void hasNoParagraphs() {
            assertTrue(doc.paragraphs().isEmpty());
        }

        @Test
        void paragraphOneIsMissing() {
            assertTrue(message(() -> doc.paragraph(1)).contains("No paragraph 1"));
        }

        @Test
        void rootIsUntitled() {
            assertEquals("", doc.root().title().plainText());
            assertSame(doc.root(), doc.section(null));
        }

        @Test
        void topLevelSectionIsMissing() {
            assertEquals("No section 1", message(() -> doc.section(SectionPath.parse("1"))));
        }

        @Test
        void outlineSaysEmpty() {
            assertEquals("(empty document)", doc.outline());
        }
    }

    // ---------------------------------------------------------------------------------------
    // A nested document
    // ---------------------------------------------------------------------------------------

    /**
     * Builds:
     * <pre>
     * 1 Intro
     *   p1 "alpha"
     *   1.1 Background
     *     p2 "beta"
     *     1.1.1 Detail
     *       p3 "gamma"
     *   p4 "delta"          (after the subsection, still inside section 1)
     * 2 Methods
     *   p5 "epsilon"
     * 3 Empty
     * </pre>
     */
    @Nested
    class NestedDocument {

        private Document doc;
        private Section intro;
        private Section background;
        private Section detail;
        private Section methods;
        private Section empty;
        private Paragraph alpha;
        private Paragraph beta;
        private Paragraph gamma;
        private Paragraph delta;
        private Paragraph epsilon;

        @BeforeEach
        void build() {
            doc = new Document();
            intro = section("Intro");
            background = section("Background");
            detail = section("Detail");
            methods = section("Methods");
            empty = section("Empty");
            alpha = para("alpha");
            beta = para("beta");
            gamma = para("gamma");
            delta = para("delta");
            epsilon = para("epsilon");

            doc.root().add(intro);
            intro.add(alpha);
            intro.add(background);
            background.add(beta);
            background.add(detail);
            detail.add(gamma);
            intro.add(delta);
            doc.root().add(methods);
            methods.add(epsilon);
            doc.root().add(empty);
        }

        @Test
        void paragraphsAreNumberedInDocumentOrderAcrossAllSections() {
            assertSame(alpha, doc.paragraph(1));
            assertSame(beta, doc.paragraph(2));
            assertSame(gamma, doc.paragraph(3));
            assertSame(delta, doc.paragraph(4));
            assertSame(epsilon, doc.paragraph(5));
            assertEquals(List.of(alpha, beta, gamma, delta, epsilon), doc.paragraphs());
        }

        @Test
        void numberOfIsTheInverseOfParagraph() {
            for (int n = 1; n <= 5; n++) {
                assertEquals(n, doc.numberOf(doc.paragraph(n)));
            }
        }

        @Test
        void numberOfAParagraphNotInTheDocumentIsMinusOne() {
            assertEquals(-1, doc.numberOf(para("alpha")));
        }

        @Test
        void numberingFollowsAStructureChange() {
            Paragraph inserted = para("new");
            intro.add(0, inserted);
            assertSame(inserted, doc.paragraph(1));
            assertSame(alpha, doc.paragraph(2));
            assertSame(epsilon, doc.paragraph(6));

            intro.remove(inserted);
            assertSame(alpha, doc.paragraph(1));
        }

        @Test
        void lastParagraphIsFoundAndOnePastItIsMissing() {
            assertSame(epsilon, doc.paragraph(5));
            String msg = message(() -> doc.paragraph(6));
            assertTrue(msg.contains("No paragraph 6"), msg);
            assertTrue(msg.contains("5"), () -> "should say how many there are: " + msg);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
        void outOfRangeParagraphIsMissing(int n) {
            assertTrue(message(() -> doc.paragraph(n)).contains("No paragraph " + n));
        }

        @Test
        void sectionsAreFoundByPath() {
            assertSame(intro, doc.section(SectionPath.parse("1")));
            assertSame(background, doc.section(SectionPath.parse("1.1")));
            assertSame(detail, doc.section(SectionPath.parse("1.1.1")));
            assertSame(methods, doc.section(SectionPath.parse("2")));
            assertSame(empty, doc.section(SectionPath.parse("3")));
        }

        @Test
        void paragraphsDoNotCountAsSubsections() {
            // Intro's children are alpha, Background, delta: Background is still 1.1.
            assertSame(background, doc.section(SectionPath.parse("1.1")));
            assertEquals(List.of(background), intro.subsections());
        }

        @ParameterizedTest
        @ValueSource(strings = {"4", "1.2", "1.1.2", "1.1.1.1", "2.1", "3.1"})
        void missingSectionNamesThePath(String path) {
            assertEquals("No section " + path,
                    message(() -> doc.section(SectionPath.parse(path))));
        }

        @Test
        void outlineShowsPathsAndParagraphNumbers() {
            String expected = String.join("\n",
                    "1 Intro",
                    "  p1: \"alpha\"",
                    "  1.1 Background",
                    "    p2: \"beta\"",
                    "    1.1.1 Detail",
                    "      p3: \"gamma\"",
                    "  p4: \"delta\"",
                    "2 Methods",
                    "  p5: \"epsilon\"",
                    "3 Empty");
            assertEquals(expected, doc.outline());
        }

        @Test
        void outlineShortensLongParagraphs() {
            methods.add(para("x".repeat(100)));
            String last = doc.outline().lines()
                    .filter(l -> l.contains("p6:")).findFirst().orElseThrow();
            assertTrue(last.endsWith("...\""), last);
            assertTrue(last.length() < 60, last);
        }

        @Test
        void outlineDoesNotChangeTheDocument() {
            doc.outline();
            assertEquals(5, doc.paragraphs().size());
            assertSame(background, doc.section(SectionPath.parse("1.1")));
        }
    }

    // ---------------------------------------------------------------------------------------
    // Paragraph
    // ---------------------------------------------------------------------------------------

    @Nested
    class ParagraphTests {

        @Test
        void holdsItsText() {
            assertEquals("hello world", para("hello world").text().plainText());
            assertEquals(11, para("hello world").text().length());
        }

        @Test
        void emptyText() {
            assertEquals(0, para("").text().length());
        }

        @Test
        void collectParagraphsAddsItself() {
            Paragraph p = para("x");
            List<Paragraph> out = new ArrayList<>();
            p.collectParagraphs(out);
            assertEquals(List.of(p), out);
        }

        @Test
        void deepCopyIsIndependent() {
            Paragraph p = para("same text");
            Paragraph copy = p.deepCopy();
            assertNotSame(p, copy);
            assertNotSame(p.text(), copy.text());
            assertEquals("same text", copy.text().plainText());
        }

        @Test
        void sectionDeepCopyCopiesEveryParagraph() {
            Section s = section("S");
            Paragraph p = para("a");
            s.add(p);
            Section copy = s.deepCopy();
            List<Paragraph> copied = new ArrayList<>();
            copy.collectParagraphs(copied);
            assertEquals(1, copied.size());
            assertNotSame(p, copied.get(0));
            assertEquals("a", copied.get(0).text().plainText());
        }
    }

    // ---------------------------------------------------------------------------------------
    // Listeners (Observer)
    // ---------------------------------------------------------------------------------------

    @Nested
    class Listeners {

        private final Document doc = new Document();
        private final List<String> calls = new ArrayList<>();

        @Test
        void calledInRegistrationOrderWithTheSameEvent() {
            List<DocumentEvent> seen = new ArrayList<>();
            doc.addListener(e -> { calls.add("first"); seen.add(e); });
            doc.addListener(e -> { calls.add("second"); seen.add(e); });
            doc.addListener(e -> { calls.add("third"); seen.add(e); });

            DocumentEvent event = new DocumentEvent(DocumentEvent.Kind.TEXT);
            doc.fireChanged(event);

            assertEquals(List.of("first", "second", "third"), calls);
            assertEquals(3, seen.size());
            seen.forEach(e -> assertSame(event, e));
        }

        @Test
        void calledOncePerFire() {
            doc.addListener(e -> calls.add(e.kind().name()));
            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.STRUCTURE));
            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.FORMAT));
            assertEquals(List.of("STRUCTURE", "FORMAT"), calls);
        }

        @Test
        void fireWithNoListenersIsFine() {
            assertDoesNotThrow(() ->
                    doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.RESTORE)));
        }

        @Test
        void sameListenerAddedTwiceIsCalledTwice() {
            DocumentListener l = e -> calls.add("x");
            doc.addListener(l);
            doc.addListener(l);
            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
            assertEquals(2, calls.size());
        }

        @Test
        void listenerAddedDuringAFireIsCalledFromTheNextFire() {
            doc.addListener(e -> {
                calls.add("outer");
                if (calls.size() == 1) {
                    doc.addListener(e2 -> calls.add("inner"));
                }
            });
            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
            assertEquals(List.of("outer"), calls);
            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.TEXT));
            assertEquals(List.of("outer", "outer", "inner"), calls);
        }

        @Test
        void nullListenerIsRejected() {
            assertThrows(NullPointerException.class, () -> doc.addListener(null));
        }

        @Test
        void eventKeepsItsKind() {
            for (DocumentEvent.Kind k : DocumentEvent.Kind.values()) {
                assertEquals(k, new DocumentEvent(k).kind());
            }
            assertThrows(NullPointerException.class, () -> new DocumentEvent(null));
        }

        @Test
        void commandsNotifyOnExecuteAndUndo() {
            doc.addListener(e -> calls.add(e.kind().name()));

            AddSectionCommand addSection = new AddSectionCommand(null, "Intro");
            addSection.validate(doc);
            addSection.execute(doc);
            AddParagraphCommand addPara =
                    new AddParagraphCommand(SectionPath.parse("1"), "text");
            addPara.validate(doc);
            addPara.execute(doc);
            addPara.undo(doc);
            addSection.undo(doc);

            assertEquals(List.of("STRUCTURE", "STRUCTURE", "STRUCTURE", "STRUCTURE"), calls);
            assertEquals("(empty document)", doc.outline());
        }
    }

    // ---------------------------------------------------------------------------------------
    // Mementos (createMemento / restore)
    // ---------------------------------------------------------------------------------------

    @Nested
    class Mementos {

        private Document doc;

        @BeforeEach
        void build() {
            doc = new Document();
            Section intro = section("Intro");
            intro.add(para("alpha"));
            doc.root().add(intro);
        }

        @Test
        void restoreBringsBackTheSavedState() {
            String before = doc.outline();
            DocumentMemento m = doc.createMemento();

            doc.root().add(section("Later"));
            doc.section(SectionPath.parse("1")).add(para("beta"));

            doc.restore(m);
            assertEquals(before, doc.outline());
        }

        @Test
        void editingAfterSaveNeverChangesTheSavedCopy() {
            DocumentMemento m = doc.createMemento();
            String saved = doc.outline();

            // Edit the live nodes that existed when the memento was taken.
            doc.section(SectionPath.parse("1")).add(para("changed"));
            doc.root().remove(doc.section(SectionPath.parse("1")));

            doc.restore(m);
            assertEquals(saved, doc.outline());
        }

        @Test
        void editingAfterRestoreNeverChangesTheSavedCopy() {
            DocumentMemento m = doc.createMemento();
            String saved = doc.outline();

            doc.restore(m);
            doc.section(SectionPath.parse("1")).add(para("after restore"));

            doc.restore(m);
            assertEquals(saved, doc.outline());
        }

        @Test
        void restoredNodesAreFreshCopies() {
            Paragraph before = doc.paragraph(1);
            DocumentMemento m = doc.createMemento();
            doc.restore(m);
            assertNotSame(before, doc.paragraph(1));
            assertEquals("alpha", doc.paragraph(1).text().plainText());
            assertEquals(-1, doc.numberOf(before));
        }

        @Test
        void restoringAnEmptyDocumentEmptiesIt() {
            DocumentMemento empty = new Document().createMemento();
            doc.restore(empty);
            assertEquals("(empty document)", doc.outline());
            assertTrue(doc.paragraphs().isEmpty());
        }

        @Test
        void oneMementoCanBeRestoredIntoAnotherDocument() {
            Document other = new Document();
            other.restore(doc.createMemento());
            assertEquals(doc.outline(), other.outline());
            assertNotSame(doc.paragraph(1), other.paragraph(1));
        }

        @Test
        void restoreDoesNotFireButListenersSurviveIt() {
            List<String> calls = new ArrayList<>();
            doc.addListener(e -> calls.add(e.kind().name()));

            doc.restore(doc.createMemento());
            assertTrue(calls.isEmpty(), "restore leaves notifying to the command");

            doc.fireChanged(new DocumentEvent(DocumentEvent.Kind.RESTORE));
            assertEquals(List.of("RESTORE"), calls);
        }

        @Test
        void nullMementoIsRejected() {
            assertThrows(NullPointerException.class, () -> doc.restore(null));
        }

        @Test
        void commandUndoStillWorksAfterARestoreRoundTrip() {
            // Simulates: add section, add paragraph, restore a checkpoint, undo the restore,
            // then undo the two adds. The live nodes are copies by then.
            Document d = new Document();
            DocumentMemento emptyCheckpoint = d.createMemento();

            AddSectionCommand addSection = new AddSectionCommand(null, "Intro");
            addSection.validate(d);
            addSection.execute(d);
            AddParagraphCommand addPara =
                    new AddParagraphCommand(SectionPath.parse("1"), "text");
            addPara.validate(d);
            addPara.execute(d);

            DocumentMemento current = d.createMemento();   // RestoreCheckpointCommand.execute
            d.restore(emptyCheckpoint);
            d.restore(current);                            // RestoreCheckpointCommand.undo

            addPara.undo(d);
            assertTrue(d.paragraphs().isEmpty());
            addSection.undo(d);
            assertEquals("(empty document)", d.outline());

            addSection.execute(d);                         // redo still works
            addPara.execute(d);
            assertEquals("text", d.paragraph(1).text().plainText());
        }
    }
}
