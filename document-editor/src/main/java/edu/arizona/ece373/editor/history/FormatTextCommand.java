/*
 * FormatTextCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Sam        Reviewer: Jack
 * When:      Week of Nov 2
 * Pattern:   Command (concrete command)
 * Satisfies: 6.5.2, 6.5.3, 6.5.4
 * Design doc: UC3; Section 5.6
 *
 * PURPOSE: Applies one formatting change to a range of characters.
 *
 * WHAT TO DO:
 *   1. validate: range and FormatChange value.
 *   2. execute: keep the old runs returned by applyFormat(); undo: put the old runs back.
 *   3. Characters themselves never change (6.5.4).
 *
 * TESTED BY: FormatTextCommandTest
 *
 * NOTE: Lives in Jack's history package but is Sam's (character formatting subsystem in the
 *   work plan).
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
