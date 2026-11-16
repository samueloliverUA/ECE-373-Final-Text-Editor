package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.model.SectionPath;
import org.junit.jupiter.api.Test;



class AddParagraphCommandTest {

        //helpers (the only places that touch other classes constructors)

        //Makes a document with one top-level section per title
        private static Document docWithSections(String... titles) {
            Document d = new Document();
            for (String title : titles) {
                new AddSectionCommand(null, title).execute(d);
            }
            return d;
        }

        private static SectionPath path(String dotted) {
            return SectionPath.parse(dotted);
        }

        //Appends a paragraph without going through the command under test's checks
        private static void addPara(Document d, String section, String text) {
            new AddParagraphCommand(path(section), text).execute(d);
        }

        private static String text(Document d, int n) {
            return d.paragraph(n).text().plainText();
        }

        //main case

        @Test
        void addsParagraphToAnEmptySection() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "Hello world");

            c.validate(d);
            c.execute(d);

            assertEquals("Hello world", text(d, 1));
            assertEquals(1, c.addedNumber());
        }

        @Test
        void addsToTheEndOfTheSection() {
            Document d = docWithSections("Intro");
            addPara(d, "1", "first");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "second");

            c.execute(d);

            assertEquals("first", text(d, 1));
            assertEquals("second", text(d, 2));
            assertEquals(2, c.addedNumber());
        }

        @Test
        void numberCountsParagraphsAcrossSections() {
            Document d = docWithSections("One", "Two");
            addPara(d, "1", "a");
            addPara(d, "2", "b");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "new");

            c.execute(d);

            // New paragraph goes at the end of section 1, so "b" moves to p3.
            assertEquals(2, c.addedNumber());
            assertEquals("a", text(d, 1));
            assertEquals("new", text(d, 2));
            assertEquals("b", text(d, 3));
        }

        @Test
        void addsToASubsection() {
            Document d = docWithSections("One");
            new AddSectionCommand(path("1"), "One point one").execute(d);
            AddParagraphCommand c = new AddParagraphCommand(path("1.1"), "nested");

            c.validate(d);
            c.execute(d);

            assertEquals("nested", text(d, 1));
            assertSame(d.paragraph(1), d.paragraph(c.addedNumber()));
        }

        //undo round trip: execute then undo gives exactly the original

        @Test
        void undoRoundTripRestoresTheDocumentExactly() {
            Document d = docWithSections("One", "Two");
            addPara(d, "1", "a");
            new AddSectionCommand(path("1"), "One point one").execute(d);
            addPara(d, "1.1", "b");
            addPara(d, "2", "c");
            // Same objects means same text AND same formatting: nothing was copied or rebuilt
            var p1 = d.paragraph(1);
            var p2 = d.paragraph(2);
            var p3 = d.paragraph(3);
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "temporary");

            c.execute(d);
            c.undo(d);

            assertSame(p1, d.paragraph(1));
            assertSame(p2, d.paragraph(2));
            assertSame(p3, d.paragraph(3));
            assertEquals("a", text(d, 1));
            assertEquals("b", text(d, 2));
            assertEquals("c", text(d, 3));
            assertThrows(EditException.class, () -> d.paragraph(4));
        }

        @Test
        void repeatedRoundTripsLeaveTheDocumentUnchanged() {
            Document d = docWithSections("Intro");
            addPara(d, "1", "keep");
            var kept = d.paragraph(1);
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "x");

            for (int i = 0; i < 3; i++) {
                c.execute(d);
                c.undo(d);
            }

            assertSame(kept, d.paragraph(1));
            assertThrows(EditException.class, () -> d.paragraph(2));
        }

        // undo/redo

        @Test
        void undoRemovesTheParagraph() {
            Document d = docWithSections("Intro");
            addPara(d, "1", "keep");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "remove me");
            c.execute(d);

            c.undo(d);

            assertEquals("keep", text(d, 1));
            assertThrows(EditException.class, () -> d.paragraph(2));
        }

        @Test
        void undoTheOnlyParagraphLeavesTheSectionEmpty() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "only");
            c.execute(d);

            c.undo(d);

            assertThrows(EditException.class, () -> d.paragraph(1));
        }

        @Test
        void redoAfterUndoPutsTheSameParagraphBack() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "again");
            c.execute(d);
            var first = d.paragraph(1);

            c.undo(d);
            c.execute(d);

            assertEquals("again", text(d, 1));
            assertSame(first, d.paragraph(1));
        }

        @Test
        void worksThroughHistory() {
            Document d = docWithSections("Intro");
            History h = new History(d);

            h.perform(new AddParagraphCommand(path("1"), "one"));
            h.perform(new AddParagraphCommand(path("1"), "two"));
            h.undo();

            assertEquals("one", text(d, 1));
            assertThrows(EditException.class, () -> d.paragraph(2));

            h.redo();
            assertEquals("two", text(d, 2));
        }

        //invalid input: rejected by validate, document unchanged

        @Test
        void validateRejectsAMissingTopLevelSection() {
            Document d = docWithSections("Intro");
            addPara(d, "1", "existing");
            AddParagraphCommand c = new AddParagraphCommand(path("2"), "nope");

            EditException e = assertThrows(EditException.class, () -> c.validate(d));

            assertTrue(e.getMessage().contains("2"), e.getMessage());
            assertEquals("existing", text(d, 1));
            assertThrows(EditException.class, () -> d.paragraph(2));
            assertEquals(-1, c.addedNumber());
        }

        @Test
        void validateRejectsAMissingSubsection() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1.3"), "nope");

            assertThrows(EditException.class, () -> c.validate(d));
            assertThrows(EditException.class, () -> d.paragraph(1));
        }

        @Test
        void validateRejectsAnyPathInAnEmptyDocument() {
            Document d = new Document();
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "nope");

            assertThrows(EditException.class, () -> c.validate(d));
        }

        @Test
        void failedValidateFiresNoEvent() {
            Document d = docWithSections("Intro");
            int[] events = {0};
            d.addListener(e -> events[0]++);
            AddParagraphCommand c = new AddParagraphCommand(path("5"), "nope");

            assertThrows(EditException.class, () -> c.validate(d));

            assertEquals(0, events[0]);
        }

        @Test
        void historyDoesNotRecordARejectedAdd() {
            Document d = docWithSections("Intro");
            History h = new History(d);

            assertThrows(EditException.class,
                    () -> h.perform(new AddParagraphCommand(path("9"), "nope")));

            assertFalse(h.canUndo());
        }

        //boundaries: position 0 (empty section) and position = length (end of
        //section) are covered by addsParagraphToAnEmptySection and
        //addsToTheEndOfTheSection above; empty paragraph below

        @Test
        void emptyTextMakesAnEmptyParagraph() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "");

            c.execute(d);

            assertEquals(0, d.paragraph(1).text().length());
        }

        @Test
        void nullTextIsTreatedAsEmpty() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), null);

            c.execute(d);

            assertEquals("", text(d, 1));
        }

        //events and describe

        @Test
        void executeAndUndoEachFireOneEvent() {
            Document d = docWithSections("Intro");
            int[] events = {0};
            d.addListener(e -> events[0]++);
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "x");

            c.execute(d);
            assertEquals(1, events[0]);

            c.undo(d);
            assertEquals(2, events[0]);
        }

        @Test
        void describeChangesAfterExecute() {
            Document d = docWithSections("Intro");
            AddParagraphCommand c = new AddParagraphCommand(path("1"), "x");

            assertEquals(-1, c.addedNumber());
            assertEquals("add paragraph to section 1", c.describe());

            c.execute(d);

            assertEquals("added paragraph 1 to section 1", c.describe());
        }
    }
