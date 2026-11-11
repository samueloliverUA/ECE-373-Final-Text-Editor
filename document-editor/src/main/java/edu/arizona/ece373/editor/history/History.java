/*
 * History.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Pattern:   Command (invoker)
 * Satisfies: 6.4.1-6.4.4, 6.8.7
 * Design doc: Fig 2a, Figs 3-5, Fig 7; Sections 5.2, 5.3
 *
 * PURPOSE: Undo and redo stacks with no small limit, plus grouping for macros.
 *
 * MEMBERS (from the class diagram):
 *   - -undoStack : Deque<EditCommand>
 *   - -redoStack : Deque<EditCommand>
 *   - +perform(c : EditCommand)
 *   - +undo()
 *   - +redo()
 *   - +beginGroup(name : String)
 *   - +endGroup()
 *   - +abortGroup()
 *
 * WHAT TO DO:
 *   1. perform(c): c.validate(doc), c.execute(doc), then push on undoStack and clear redoStack.
 *      If a group is open, add c to the group instead of the stack.
 *   2. undo(): nothing to undo -> message. Otherwise pop, c.undo(doc), push on redoStack.
 *   3. redo(): nothing to redo -> message. Otherwise pop, c.execute(doc), push on undoStack.
 *   4. Use ArrayDeque; no size cap (6.4.3).
 *   5. beginGroup(name) opens a CompositeCommand; endGroup() pushes it as ONE entry (skip if
 *      empty); abortGroup() undoes the open group and discards it.
 *
 * TESTED BY: HistoryTest
 *
 * DONE WHEN: 1,000 edits can all be undone and redone; a new edit after undo empties redo.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
