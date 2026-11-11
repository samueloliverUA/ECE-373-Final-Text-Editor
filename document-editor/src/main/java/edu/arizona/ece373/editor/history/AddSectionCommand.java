/*
 * AddSectionCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Pattern:   Command (concrete command)
 * Satisfies: 6.2.2, 6.2.4
 * Design doc: UC1 steps 1-2
 *
 * PURPOSE: Adds a section (top-level or under a parent path) with a title.
 *
 * WHAT TO DO:
 *   1. validate: parent path exists (if given); title not empty.
 *   2. execute: create the Section and add it at the end of the parent; remember where.
 *   3. undo: remove that exact Section.
 *
 * TESTED BY: AddSectionCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
