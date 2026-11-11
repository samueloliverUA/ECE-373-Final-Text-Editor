/*
 * SplitParagraphCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.5
 * Design doc: UC1 alternate flow 4a
 *
 * PURPOSE: Splits a paragraph at an offset into two paragraphs in the same section.
 *
 * WHAT TO DO:
 *   1. validate: 0 <= offset <= length.
 *   2. execute: text.splitAt(offset) -> new Paragraph inserted right after the original.
 *   3. undo: append the new paragraph's text back and remove it.
 *
 * TESTED BY: SplitParagraphCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
