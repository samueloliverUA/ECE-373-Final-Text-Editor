/*
 * DeleteTextCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.2
 * Design doc: Section 5.2
 *
 * PURPOSE: Deletes a range of text, remembering it (with formatting) for undo.
 *
 * WHAT TO DO:
 *   1. validate the range.
 *   2. execute: keep the FormattedText returned by cut(); undo: insert it back at start.
 *
 * TESTED BY: DeleteTextCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
