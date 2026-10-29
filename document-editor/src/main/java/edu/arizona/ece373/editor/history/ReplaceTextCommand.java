/*
 * ReplaceTextCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.3, 6.8.2, 6.8.3
 * Design doc: Figs 4, 5
 *
 * PURPOSE: Replaces a range with new text. Also used by replace-first and Replace All.
 *
 * WHAT TO DO:
 *   1. validate the range.
 *   2. execute: cut the old text (keep it), insert the new text; undo: cut the new text, insert
 *      the old.
 *   3. Inserted text takes the formatting of the first replaced character (decide and note it
 *      in JavaDoc).
 *
 * TESTED BY: ReplaceTextCommandTest, ReplaceAllTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
