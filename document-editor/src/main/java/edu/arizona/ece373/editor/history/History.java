package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo and redo stacks with no size limit (6.4.3).
 *
 * <p>TODO (week of Nov 16): {@code beginGroup}/{@code endGroup}/{@code abortGroup} for macro
 * replay, using CompositeCommand.
 */
public final class History {

    private final Document doc;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();

    /**
     * @param doc the document every command runs against
     */
    public History(Document doc) {
        this.doc = doc;
    }

    /**
     * Validates and executes {@code c}, then records it. A new change discards the redo path.
     *
     * @param c the command
     * @throws edu.arizona.ece373.editor.model.EditException if validation fails; nothing is
     *     changed or recorded in that case
     */
    public void perform(EditCommand c) {
        c.validate(doc);
        c.execute(doc);
        undoStack.push(c);
        redoStack.clear();
    }

    /** @return a message saying what was undone, or that there was nothing to undo */
    public String undo() {
        if (undoStack.isEmpty()) {
            return "Nothing to undo";
        }
        EditCommand c = undoStack.pop();
        c.undo(doc);
        redoStack.push(c);
        return "Undid: " + c.describe();
    }

    /** @return a message saying what was redone, or that there was nothing to redo */
    public String redo() {
        if (redoStack.isEmpty()) {
            return "Nothing to redo";
        }
        EditCommand c = redoStack.pop();
        c.execute(doc);
        undoStack.push(c);
        return "Redid: " + c.describe();
    }

    /** @return true if {@link #undo} would do something */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** @return true if {@link #redo} would do something */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}
