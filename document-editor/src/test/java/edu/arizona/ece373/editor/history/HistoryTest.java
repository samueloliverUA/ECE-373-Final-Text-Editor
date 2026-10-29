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
