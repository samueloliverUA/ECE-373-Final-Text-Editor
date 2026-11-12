package edu.arizona.ece373.editor.model;

import java.util.Objects;

/**
 * An opaque saved copy of the whole document, used for checkpoints (Memento pattern).
 *
 * <p>Holds a deep copy of the root section taken when the memento is created. Only
 * {@link Document} can read it back, and it always hands out a fresh copy, so neither later
 * edits nor restoring it can change the snapshot.
 *
 * <p>Satisfies 6.4.5, 6.4.6.
 */
public final class DocumentMemento {

    private final Section snapshot;

    /**
     * @param snapshot a deep copy of the root that nothing else references
     */
    DocumentMemento(Section snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    /** @return the saved root; package-private so only Document reads it, and never mutates it */
    Section snapshot() {
        return snapshot;
    }
}
