package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.DocumentMemento;
import edu.arizona.ece373.editor.model.EditException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Named checkpoints: each name maps to a saved copy of the whole document (6.4.x,
 * design doc Section 5.5).
 *
 * <p>Saving is not an edit, so it is not undoable. Restoring is, through
 * {@link RestoreCheckpointCommand}.
 */
public final class CheckpointStore {

    // TreeMap keeps the names sorted, so 'checkpoint list' output is repeatable.
    private final Map<String, DocumentMemento> saved = new TreeMap<>();

    /** Creates an empty checkpoint store. */
    public CheckpointStore() {
    }

    /**
     * Saves the document under a name. An existing checkpoint with that name is
     * overwritten.
     *
     * @param name the checkpoint name; must not be blank
     * @param d the document to save
     * @throws EditException if the name is null or blank
     */
    public void save(String name, Document d) {
        if (name == null || name.isBlank()) {
            throw new EditException("Checkpoint name cannot be empty");
        }
        saved.put(name, d.createMemento());
    }

    /**
     * Looks up a saved checkpoint by name.
     *
     * @param name a checkpoint name
     * @return the saved copy
     * @throws EditException if there is no checkpoint with that name; the message
     *     lists the saved names
     */
    public DocumentMemento get(String name) {
        DocumentMemento m = null;
        if (name != null) { // TreeMap.get(null) would throw NullPointerException
            m = saved.get(name);
        }
        if (m == null) {
            if (saved.isEmpty()) {
                throw new EditException("No checkpoint \"" + name + "\"; no checkpoints saved");
            }
            throw new EditException("No checkpoint \"" + name + "\"; saved: "
                    + String.join(", ", saved.keySet()));
        }
        return m;
    }

    /**
     * Checks whether a checkpoint with this name exists.
     *
     * @param name a checkpoint name
     * @return true if a checkpoint with this name exists
     */
    public boolean has(String name) {
        return name != null && saved.containsKey(name);
    }

    /**
     * Lists the saved checkpoint names.
     *
     * @return the checkpoint names, sorted
     */
    public List<String> names() {
        return new ArrayList<>(saved.keySet());
    }
}
