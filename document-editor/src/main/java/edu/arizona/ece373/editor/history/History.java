package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo and redo stacks with no size limit (6.4.3).
 *
 * <p>{@code beginGroup}, {@code endGroup} and {@code abortGroup} collect the commands of a macro
 * replay into one {@link CompositeCommand}, so a single undo reverses the whole replay.
 */
public final class History {

    private final Document doc;
    private final Deque<EditCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditCommand> redoStack = new ArrayDeque<>();
    private CompositeCommand group;
    /**
     * Creates empty undo and redo stacks for a document.
     *
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
        if (group != null) {
            group.add(c);
        }
        else {
            undoStack.push(c);
            redoStack.clear();
        }
    }

    /**
     * Undoes the most recent change.
     *
     * @return a message saying what was undone, or that there was nothing to undo
     */
    public String undo() {
        if (group != null) {
            throw new IllegalStateException("cannot undo while a group is open");
        }
        if (undoStack.isEmpty()) {
            return "Nothing to undo";
        }
        EditCommand c = undoStack.pop();
        c.undo(doc);
        redoStack.push(c);
        return "Undid: " + c.describe();
    }

    /**
     * Redoes the most recently undone change.
     *
     * @return a message saying what was redone, or that there was nothing to redo
     */
    public String redo() {
        if (group != null) {
            throw new IllegalStateException("cannot redo while a group is open");
        }
        if (redoStack.isEmpty()) {
            return "Nothing to redo";
        }
        EditCommand c = redoStack.pop();
        c.execute(doc);
        undoStack.push(c);
        return "Redid: " + c.describe();
    }

    /**
     * Opens a group: every command performed until {@link #endGroup} becomes one history entry.
     *
     * @param name the group's name for messages, e.g. {@code macro tidy}
     * @throws IllegalStateException if a group is already open (groups do not nest)
     */
    public void beginGroup(String name) {
        if (group != null) {
            throw new IllegalStateException("a group is already open");
        }
        group = new CompositeCommand(name);
    }

    /**
     * Closes the open group and records it as one entry. An empty group records nothing.
     *
     * @throws IllegalStateException if no group is open
     */
    public void endGroup() {
        if (group == null) {
            throw new IllegalStateException("endGroup called with no open group");
        }
        CompositeCommand g = group;
        group = null;
        if (!g.isEmpty()) {
            undoStack.push(g);
            redoStack.clear();
        }
    }

    /**
     * Undoes every command performed since {@link #beginGroup}, in reverse order, and discards the
     * group, so the document is back to how it was before the group began.
     *
     * @throws IllegalStateException if no group is open
     */
    public void abortGroup() {
        if (group == null) {
            throw new IllegalStateException("abortGroup called with no open group");
        }
        CompositeCommand g = group;
        group = null;
        g.undo(doc);
    }

    /**
     * Checks whether {@link #undo} would do something.
     *
     * @return true if {@link #undo} would do something
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks whether {@link #redo} would do something.
     *
     * @return true if {@link #redo} would do something
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Checks whether a command group is open.
     *
     * @return true between {@link #beginGroup} and {@link #endGroup} or {@link #abortGroup}
     */
    public boolean isGroupOpen() {
        return group != null;
    }
}
