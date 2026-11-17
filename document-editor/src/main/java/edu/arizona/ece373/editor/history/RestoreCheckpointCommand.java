package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentEvent;
import edu.arizona.ece373.editor.model.DocumentMemento;

/**
 * Replaces the document with a saved checkpoint, as an undoable change (6.4.6,
 * design doc Figure 2a, Section 5.5, UC2 alternate flow 4b).
 *
 * <p>Keeps two mementos (multiplicity 2 in the class diagram): the checkpoint being
 * restored, and a copy of the document as it was just before the restore, which undo
 * puts back.
 *
 * <p>The checkpoint is looked up once, on the first execute. If the name is later
 * overwritten with a new save, redo still restores the version this command used.
 */
public final class RestoreCheckpointCommand implements EditCommand {

    private final CheckpointStore store;
    private final String name;
    private DocumentMemento checkpoint;
    private DocumentMemento before;

    /**
     * @param store where the checkpoints are kept
     * @param name the checkpoint to restore
     */
    public RestoreCheckpointCommand(CheckpointStore store, String name) {
        this.store = store;
        this.name = name;
    }

    @Override
    public void validate(Document d) {
        if (checkpoint == null) {
            store.get(name); // throws "No checkpoint "x"; saved: a, b"
        }
    }

    @Override
    public void execute(Document d) {
        if (checkpoint == null) {
            checkpoint = store.get(name);
        }
        before = d.createMemento();
        d.restore(checkpoint);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.RESTORE));
    }

    @Override
    public void undo(Document d) {
        d.restore(before);
        d.fireChanged(new DocumentEvent(DocumentEvent.Kind.RESTORE));
    }

    @Override
    public String describe() {
        if (before == null) {
            return "restore checkpoint \"" + name + "\"";
        } else {
            return "restored checkpoint \"" + name + "\"";
        }
    }
}
