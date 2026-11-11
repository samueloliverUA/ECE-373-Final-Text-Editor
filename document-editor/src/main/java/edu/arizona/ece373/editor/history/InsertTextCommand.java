/*
 * InsertTextCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.1
 * Design doc: Fig 3
 *
 * PURPOSE: Inserts text at a position in a paragraph.
 *
 * WHAT TO DO:
 *   1. validate: paragraph exists and 0 <= offset <= length (Figure 3 alt fragment).
 *   2. execute: paragraph.text().insert(offset, text); undo: cut the same range back out.
 *
 * TESTED BY: InsertTextCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
