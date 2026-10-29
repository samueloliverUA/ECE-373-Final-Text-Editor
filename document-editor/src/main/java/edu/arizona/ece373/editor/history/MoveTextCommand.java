/*
 * MoveTextCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.3.4
 * Design doc: UC1 alternate flow 4c
 *
 * PURPOSE: Moves a range to another position in the same paragraph.
 *
 * WHAT TO DO:
 *   1. Both source and destination are positions in the document as it was BEFORE the move
 *      (6.3.4).
 *   2. validate: same paragraph; valid range; destination NOT inside the source range.
 *   3. execute: cut source, adjust the destination if it was after the source, insert. undo:
 *      reverse exactly.
 *
 * TESTED BY: MoveTextCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
