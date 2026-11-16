package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

// Tests History and CompositeCommand
class HistoryTest {

    private static final class Step implements EditCommand {
        private final String id;
        private final List<String> log;
        private boolean failValidate;
        private boolean failExecute;

        Step(String id, List<String> log) {
            this.id = id;
            this.log = log;
        }

        @Override
        public void validate(Document d) {
            log.add("validate " + id);
            if (failValidate) {
                throw new IllegalStateException(id + " is invalid");
            }
        }

        @Override
        public void execute(Document d) {
            if (failExecute) {
                throw new IllegalStateException(id + " failed");
            }
            log.add("execute " + id);
        }

        @Override
        public void undo(Document d) {
            log.add("undo " + id);
        }

        @Override
        public String describe() {
            return id;
        }
    }

    /** A fake EditCommand that adds an amount to a shared number, so tests can check real state. */
    private static final class Add implements EditCommand {
        private final int[] total;
        private final int amount;

        Add(int[] total, int amount) {
            this.total = total;
            this.amount = amount;
        }

        @Override
        public void validate(Document d) {
        }

        @Override
        public void execute(Document d) {
            total[0] += amount;
        }

        @Override
        public void undo(Document d) {
            total[0] -= amount;
        }

        @Override
        public String describe() {
            return "add " + amount;
        }
    }

    @Nested
    class CompositeCommandTests {

        @Test
        void executesPartsInOrder() {
            List<String> log = new ArrayList<>();
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));
            all.add(new Step("c", log));

            all.execute(null);

            assertEquals(List.of("execute a", "execute b", "execute c"), log);
        }

        @Test
        void undoesPartsInReverseOrder() {
            List<String> log = new ArrayList<>();
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));
            all.add(new Step("c", log));

            all.execute(null);
            log.clear();
            all.undo(null);

            assertEquals(List.of("undo c", "undo b", "undo a"), log);
        }

        @Test
        void redoAfterUndoRunsPartsInOrderAgain() {
            List<String> log = new ArrayList<>();
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));

            all.execute(null);
            all.undo(null);
            log.clear();
            all.execute(null);

            assertEquals(List.of("execute a", "execute b"), log);
        }

        @Test
        void validateChecksEveryPartAndChangesNothing() {
            List<String> log = new ArrayList<>();
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));
            all.add(new Step("c", log));

            all.validate(null);

            assertEquals(List.of("validate a", "validate b", "validate c"), log);
        }

        @Test
        void validateStopsAtTheFirstInvalidPart() {
            List<String> log = new ArrayList<>();
            Step b = new Step("b", log);
            b.failValidate = true;
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(b);
            all.add(new Step("c", log));

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> all.validate(null));

            assertEquals("b is invalid", e.getMessage());
            assertEquals(List.of("validate a", "validate b"), log);
        }

        @Test
        void failingPartRollsBackEarlierPartsAndRethrows() {
            List<String> log = new ArrayList<>();
            Step c = new Step("c", log);
            c.failExecute = true;
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));
            all.add(c);

            IllegalStateException e = assertThrows(IllegalStateException.class, () -> all.execute(null));

            assertEquals("c failed", e.getMessage());
            assertEquals(List.of("execute a", "execute b", "undo b", "undo a"), log);
        }

        @Test
        void firstPartFailingUndoesNothing() {
            List<String> log = new ArrayList<>();
            Step a = new Step("a", log);
            a.failExecute = true;
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(a);
            all.add(new Step("b", log));

            assertThrows(IllegalStateException.class, () -> all.execute(null));

            assertTrue(log.isEmpty());
        }

        @Test
        void describeGivesNameAndPartCount() {
            List<String> log = new ArrayList<>();
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", log));
            all.add(new Step("b", log));
            all.add(new Step("c", log));

            assertEquals("replace all (3)", all.describe());
        }

        @Test
        void emptyCompositeDoesNothing() {
            CompositeCommand empty = new CompositeCommand("macro tidy");

            empty.validate(null);
            empty.execute(null);
            empty.undo(null);

            assertTrue(empty.isEmpty());
            assertEquals(0, empty.size());
            assertEquals("macro tidy (0)", empty.describe());
        }

        @Test
        void sizeAndPartsReflectAdds() {
            List<String> log = new ArrayList<>();
            Step a = new Step("a", log);
            Step b = new Step("b", log);
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(a);
            all.add(b);

            assertFalse(all.isEmpty());
            assertEquals(2, all.size());
            assertSame(a, all.parts().get(0));
            assertSame(b, all.parts().get(1));
        }

        @Test
        void partsListIsReadOnly() {
            CompositeCommand all = new CompositeCommand("replace all");
            all.add(new Step("a", new ArrayList<>()));

            assertThrows(UnsupportedOperationException.class, () -> all.parts().clear());
        }

        @Test
        void rejectsNullAndItself() {
            CompositeCommand all = new CompositeCommand("replace all");

            assertThrows(IllegalArgumentException.class, () -> all.add(null));
            assertThrows(IllegalArgumentException.class, () -> all.add(all));
        }


        @Test
        void nestedCompositeUndoesAsOneUnit() {
            List<String> log = new ArrayList<>();
            CompositeCommand inner = new CompositeCommand("replace all");
            inner.add(new Step("a", log));
            inner.add(new Step("b", log));
            CompositeCommand outer = new CompositeCommand("macro tidy");
            outer.add(inner);
            outer.add(new Step("c", log));

            outer.execute(null);
            outer.undo(null);

            assertEquals(List.of("execute a", "execute b", "execute c",
                    "undo c", "undo b", "undo a"), log);
        }
    }

    @Nested
    class HistoryTests {

        @Test
        void undoAndRedoGoInOrder() {
            List<String> log = new ArrayList<>();
            History h = new History(null);
            h.perform(new Step("a", log));
            h.perform(new Step("b", log));

            assertEquals("Undid: b", h.undo());
            assertEquals("Undid: a", h.undo());
            assertEquals("Redid: a", h.redo());
            assertEquals("Redid: b", h.redo());
            assertEquals(List.of("validate a", "execute a", "validate b", "execute b",
                    "undo b", "undo a", "execute a", "execute b"), log);
        }
        @Test
        void nothingToUndoOrRedoGivesAMessage() {
            History h = new History(null);

            assertEquals("Nothing to undo", h.undo());
            assertEquals("Nothing to redo", h.redo());
            assertFalse(h.canUndo());
            assertFalse(h.canRedo());
        }
        @Test
        void newEditAfterUndoClearsRedo() {
            int[] total = {0};
            History h = new History(null);
            h.perform(new Add(total, 1));
            h.undo();
            assertTrue(h.canRedo());

            h.perform(new Add(total, 5));

            assertFalse(h.canRedo());
            assertEquals("Nothing to redo", h.redo());
            assertEquals(5, total[0]);
        }
        @Test
        void failedValidateIsNotExecutedOrPushed() {
            List<String> log = new ArrayList<>();
            Step bad = new Step("bad", log);
            bad.failValidate = true;
            History h = new History(null);

            assertThrows(IllegalStateException.class, () -> h.perform(bad));

            assertEquals(List.of("validate bad"), log);
            assertFalse(h.canUndo());
        }
        @Test
        void thousandUndos() {
            int[] total = {0};
            History h = new History(null);
            for (int i = 0; i < 1000; i++) {
                h.perform(new Add(total, 1));
            }
            assertEquals(1000, total[0]);

            for (int i = 0; i < 1000; i++) {
                h.undo();
            }
            assertEquals(0, total[0]);
            assertFalse(h.canUndo());

            for (int i = 0; i < 1000; i++) {
                h.redo();
            }
            assertEquals(1000, total[0]);
            assertFalse(h.canRedo());
        }
        @Test
        void groupIsOneEntry() {
            int[] total = {0};
            History h = new History(null);
            h.beginGroup("macro tidy");
            h.perform(new Add(total, 1));
            h.perform(new Add(total, 2));
            h.perform(new Add(total, 3));
            h.endGroup();
            assertEquals(6, total[0]);

            assertEquals("Undid: macro tidy (3)", h.undo());

            assertEquals(0, total[0]);
            assertFalse(h.canUndo());
            assertEquals("Redid: macro tidy (3)", h.redo());
            assertEquals(6, total[0]);
        }
        @Test
        void abortGroupRestoresTheState() {
            int[] total = {10};
            History h = new History(null);
            h.beginGroup("macro tidy");
            h.perform(new Add(total, 1));
            h.perform(new Add(total, 2));

            h.abortGroup();

            assertEquals(10, total[0]);
            assertFalse(h.isGroupOpen());
            assertFalse(h.canUndo());
        }
        @Test
        void failingStepThenAbortLeavesDocumentAsBefore() {
            int[] total = {0};
            List<String> log = new ArrayList<>();
            Step bad = new Step("bad", log);
            bad.failValidate = true;
            History h = new History(null);
            h.beginGroup("macro tidy");
            h.perform(new Add(total, 4));

            assertThrows(IllegalStateException.class, () -> h.perform(bad));
            h.abortGroup();

            assertEquals(0, total[0]);
            assertFalse(h.canUndo());
        }
        @Test
        void emptyGroupAddsNothing() {
            int[] total = {0};
            History h = new History(null);
            h.perform(new Add(total, 1));
            h.undo();

            h.beginGroup("macro empty");
            h.endGroup();

            assertFalse(h.canUndo());
            assertTrue(h.canRedo());
        }

        @Test
        void undoAndRedoAreRejectedWhileAGroupIsOpen() {
            History h = new History(null);
            h.beginGroup("macro tidy");

            assertThrows(IllegalStateException.class, h::undo);
            assertThrows(IllegalStateException.class, h::redo);
        }
        @Test
        void groupsDoNotNestAndMustBeOpenToClose() {
            History h = new History(null);

            assertThrows(IllegalStateException.class, h::endGroup);
            assertThrows(IllegalStateException.class, h::abortGroup);
            h.beginGroup("outer");
            assertThrows(IllegalStateException.class, () -> h.beginGroup("inner"));
        }
    }
}
