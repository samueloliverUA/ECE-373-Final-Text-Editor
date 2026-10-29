/*
 * EditCommand.java   (interface)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Pattern:   Command
 * Satisfies: 6.4.1, 6.1.4, 6.9.5
 * Design doc: Fig 2a; Sections 5.2, 5.7
 *
 * PURPOSE: One undoable change to the document.
 *
 * MEMBERS (from the class diagram):
 *   - +validate(d : Document)
 *   - +execute(d : Document)
 *   - +undo(d : Document)
 *
 * WHAT TO DO:
 *   1. Declare validate(d), execute(d), undo(d).
 *   2. Rule for every implementation: ALL checks that can fail go in validate; execute only
 *      does work already known to be valid (Section 5.7).
 *   3. Rule: execute and undo each call d.fireChanged(...) when done (Figure 3).
 *   4. Suggested addition: String describe() for messages like 'Undid: replace all (6)'.
 *
 * TESTED BY: HistoryTest, every *CommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
