/*
 * DocumentMemento.java   (class)
 * Package:   edu.arizona.ece373.editor.model
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Nov 9
 * Pattern:   Memento
 * Satisfies: 6.4.5, 6.4.6
 * Design doc: Fig 2b; Section 5.5
 *
 * PURPOSE: An opaque saved copy of the whole document, used for checkpoints.
 *
 * WHAT TO DO:
 *   1. Hold a deep copy of the root Section (root.deepCopy()).
 *   2. Make the accessor package-private so only Document can read it back; nobody else can
 *      change the snapshot.
 *
 * TESTED BY: CheckpointStoreTest, RestoreCheckpointCommandTest
 *
 * DONE WHEN: Editing the document after a save never changes the saved copy.
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */
