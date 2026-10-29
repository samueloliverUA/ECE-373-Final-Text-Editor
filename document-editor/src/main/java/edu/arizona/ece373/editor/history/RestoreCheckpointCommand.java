/*
 * RestoreCheckpointCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 9
 * Pattern:   Command (concrete command)
 * Satisfies: 6.4.6
 * Design doc: Fig 2a; Section 5.5; UC2 alternate flow 4b
 *
 * PURPOSE: Replaces the document with a saved checkpoint, as an undoable change.
 *
 * WHAT TO DO:
 *   1. validate: the checkpoint name exists.
 *   2. execute: save a memento of the CURRENT document, then d.restore(checkpoint).
 *   3. undo: d.restore(the saved current memento). Keep both mementos (diagram multiplicity 2).
 *
 * TESTED BY: RestoreCheckpointCommandTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
