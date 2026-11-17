package edu.arizona.ece373.editor.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link Section} (adding and removing children, nested sections, copying) and
 * {@link SectionPath} (parsing, rejecting bad input, building child paths).
 *
 * <p>Satisfies 6.2.2.
 */
class SectionTest {

    private static Paragraph para(String text) {
        return new Paragraph(new FormattedText(text));
    }

    private static Section section(String title) {
        return new Section(new FormattedText(title));
    }

    private static List<String> paragraphTexts(DocumentNode n) {
        List<Paragraph> out = new ArrayList<>();
        n.collectParagraphs(out);
        List<String> texts = new ArrayList<>();
        for (Paragraph p : out) {
            texts.add(p.text().plainText());
        }
        return texts;
    }

    // ---------------------------------------------------------------------------------------
    // Section
    // ---------------------------------------------------------------------------------------

    @Nested
    class Children {

        @Test
        void newSectionIsEmpty() {
            Section s = section("Intro");
            assertEquals("Intro", s.title().plainText());
            assertTrue(s.children().isEmpty());
            assertTrue(s.subsections().isEmpty());
        }

        @Test
        void addAppendsInOrder() {
            Section s = section("S");
            Paragraph a = para("a");
            Paragraph b = para("b");
            s.add(a);
            s.add(b);
            assertEquals(List.of(a, b), s.children());
        }

        @Test
        void addAtIndexInsertsAtStartMiddleAndEnd() {
            Section s = section("S");
            Paragraph a = para("a");
            Paragraph b = para("b");
            Paragraph c = para("c");
            Paragraph d = para("d");
            s.add(0, b);
            s.add(0, a);
            s.add(2, d);
            s.add(2, c);
            assertEquals(List.of("a", "b", "c", "d"), paragraphTexts(s));
        }

        @Test
        void addAtBadIndexThrows() {
            Section s = section("S");
            assertThrows(IndexOutOfBoundsException.class, () -> s.add(1, para("x")));
            assertThrows(IndexOutOfBoundsException.class, () -> s.add(-1, para("x")));
        }

        @Test
        void removeByIdentity() {
            Section s = section("S");
            Paragraph a = para("same");
            Paragraph b = para("same");
            s.add(a);
            s.add(b);
            s.remove(b);
            assertEquals(1, s.children().size());
            assertSame(a, s.children().get(0));
        }

        @Test
        void removeMissingChildDoesNothing() {
            Section s = section("S");
            s.add(para("a"));
            s.remove(para("a"));
            assertEquals(1, s.children().size());
        }

        @Test
        void indexOfByIdentity() {
            Section s = section("S");
            Paragraph a = para("x");
            Paragraph b = para("x");
            s.add(a);
            s.add(b);
            assertEquals(0, s.indexOf(a));
            assertEquals(1, s.indexOf(b));
            assertEquals(-1, s.indexOf(para("x")));
        }

        @Test
        void childrenListIsReadOnly() {
            Section s = section("S");
            assertThrows(UnsupportedOperationException.class, () -> s.children().add(para("x")));
        }

        @Test
        void subsectionsSkipParagraphs() {
            Section s = section("S");
            Section one = section("one");
            Section two = section("two");
            s.add(para("p"));
            s.add(one);
            s.add(para("q"));
            s.add(two);
            assertEquals(List.of(one, two), s.subsections());
        }
    }

    @Nested
    class Nesting {

        /** root: p "a"; 1 { p "b"; 1.1 { 1.1.1 { p "c" } }; p "d" }; p "e". */
        private Section tree() {
            Section root = section("");
            Section s1 = section("one");
            Section s11 = section("one.one");
            Section s111 = section("deep");
            s111.add(para("c"));
            s11.add(s111);
            s1.add(para("b"));
            s1.add(s11);
            s1.add(para("d"));
            root.add(para("a"));
            root.add(s1);
            root.add(para("e"));
            return root;
        }

        @Test
        void paragraphsCollectedInDocumentOrder() {
            assertEquals(List.of("a", "b", "c", "d", "e"), paragraphTexts(tree()));
        }

        @Test
        void removingNestedSectionRemovesItsParagraphs() {
            Section root = tree();
            Section s1 = root.subsections().get(0);
            s1.remove(s1.subsections().get(0));
            assertEquals(List.of("a", "b", "d", "e"), paragraphTexts(root));
        }

        @Test
        void deepCopyIsIndependentAtEveryLevel() {
            Section root = tree();
            Section copy = root.deepCopy();
            assertEquals(paragraphTexts(root), paragraphTexts(copy));

            Section deepOriginal = root.subsections().get(0).subsections().get(0)
                    .subsections().get(0);
            Section deepCopy = copy.subsections().get(0).subsections().get(0)
                    .subsections().get(0);
            assertNotSame(deepOriginal, deepCopy);
            assertNotSame(deepOriginal.title(), deepCopy.title());

            deepCopy.add(para("new"));
            copy.subsections().get(0).title().insert(0, "X");
            assertEquals(List.of("a", "b", "c", "d", "e"), paragraphTexts(root));
            assertEquals("one", root.subsections().get(0).title().plainText());
        }

        @Test
        void deepCopyKeepsTitleFormatting() {
            Section s = section("Title");
            s.title().applyFormat(0, 5, FormatChange.bold(true));
            assertEquals(s.title(), s.deepCopy().title());
        }
    }

    // ---------------------------------------------------------------------------------------
    // SectionPath
    // ---------------------------------------------------------------------------------------

    @Nested
    class Paths {

        @Test
        void parsesTopLevel() {
            SectionPath p = SectionPath.parse("2");
            assertEquals(1, p.depth());
            assertEquals(2, p.index(0));
            assertEquals("2", p.toString());
        }

        @Test
        void parsesNested() {
            SectionPath p = SectionPath.parse("2.1.3");
            assertEquals(3, p.depth());
            assertEquals(2, p.index(0));
            assertEquals(1, p.index(1));
            assertEquals(3, p.index(2));
            assertEquals("2.1.3", p.toString());
        }

        @Test
        void parsesLargeNumbers() {
            assertEquals("10.25", SectionPath.parse("10.25").toString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "", "2.", "a", ".1", "1..2", "2.0", "-1", "1.a", " 1",
                "1 ", "99999999999"})
        void rejectsBadPaths(String text) {
            assertThrows(IllegalArgumentException.class, () -> SectionPath.parse(text));
        }

        @Test
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> SectionPath.parse(null));
        }

        @Test
        void childOfNullIsTopLevel() {
            assertEquals(SectionPath.parse("3"), SectionPath.child(null, 3));
        }

        @Test
        void childAppendsIndex() {
            SectionPath parent = SectionPath.parse("2.1");
            assertEquals(SectionPath.parse("2.1.4"), SectionPath.child(parent, 4));
            assertEquals("2.1", parent.toString());
        }

        @Test
        void equalsAndHashCode() {
            assertEquals(SectionPath.parse("1.2"), SectionPath.parse("1.2"));
            assertEquals(SectionPath.parse("1.2").hashCode(),
                    SectionPath.parse("1.2").hashCode());
            assertNotEquals(SectionPath.parse("1.2"), SectionPath.parse("2.1"));
            assertNotEquals(SectionPath.parse("1"), SectionPath.parse("1.1"));
        }
    }
}
