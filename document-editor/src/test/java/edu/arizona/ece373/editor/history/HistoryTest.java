/*
 * HistoryTest.java   (JUnit 5 test class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Satisfies: 6.4.1-6.4.4, 6.1.4
 *
 * PURPOSE: Tests History and CompositeCommand.
 *
 * WHAT TO DO:
 *   1. undo/redo order; nothing to undo/redo messages.
 *   2. New edit after undo clears redo.
 *   3. thousandUndos: 1,000 edits all undone and redone.
 *   4. Groups: begin/end gives one entry; abort restores the state; empty group adds nothing.
 *   5. A command whose validate fails is not executed and not pushed.
 *
 * DONE WHEN: All cases pass under mvn clean test, including the boundary cases.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */

package edu.arizona.ece373.editor.history;

import static org.junit.jupiter.api.Assertions.*;

import edu.arizona.ece373.editor.model.Document;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
