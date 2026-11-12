package edu.arizona.ece373.editor.model;

import java.util.Objects;

/**
 * What happened in one change, passed to every {@link DocumentListener}. Immutable.
 */
public final class DocumentEvent {

    /** The kind of change. */
    public enum Kind {
        /** Characters were inserted, deleted, replaced or moved. */
        TEXT,
        /** Sections or paragraphs were added, removed, split or merged. */
        STRUCTURE,
        /** Formatting changed; the characters did not. */
        FORMAT,
        /** The whole document was replaced, e.g. by restoring a checkpoint. */
        RESTORE
    }

    private final Kind kind;

    /**
     * @param kind the kind of change; not null
     */
    public DocumentEvent(Kind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    /** @return the kind of change */
    public Kind kind() {
        return kind;
    }

    @Override
    public String toString() {
        return "DocumentEvent[" + kind + "]";
    }
}
