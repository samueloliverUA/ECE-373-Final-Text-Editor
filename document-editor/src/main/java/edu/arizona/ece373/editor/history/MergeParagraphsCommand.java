/*
 * MergeParagraphsCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.6
 * Design doc: UC1 alternate flow 4b
 *
 * PURPOSE: Appends the following paragraph to paragraph n and removes it.
 *
 * WHAT TO DO:
 *   1. validate: a following paragraph exists in the SAME section (else error; decide and
 *      document).
 *   2. execute: remember the boundary offset, append, remove the second paragraph.
 *   3. undo: split at the remembered offset and put the second paragraph back at its old index.
 *
 * TESTED BY: MergeParagraphsCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
