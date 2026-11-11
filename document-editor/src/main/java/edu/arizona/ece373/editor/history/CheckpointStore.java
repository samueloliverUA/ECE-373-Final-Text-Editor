/*
 * CheckpointStore.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 9
 * Satisfies: 6.4.5
 * Design doc: Fig 2a; UC2 steps 3-4
 *
 * PURPOSE: Named checkpoints: name -> DocumentMemento.
 *
 * MEMBERS (from the class diagram):
 *   - -saved : Map<String, DocumentMemento>
 *   - +save(name : String, d : Document)
 *   - +get(name : String) : DocumentMemento
 *
 * WHAT TO DO:
 *   1. save(name, d): store d.createMemento(); if the name exists, overwrite and say so.
 *   2. get(name): missing -> EditException listing the saved names.
 *   3. names(): sorted list for 'checkpoint list' (sorted so output is repeatable).
 *
 * TESTED BY: CheckpointStoreTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
