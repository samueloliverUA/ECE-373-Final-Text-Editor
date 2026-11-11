package edu.arizona.ece373.editor.app;

import edu.arizona.ece373.editor.history.EditCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;

/**
 * The single entry point into the engine (Facade) and the composition root. Every public
 * method returns a {@link Result}; no {@link EditException} escapes to the console layer.
 *
 * <p>Week 1 skeleton: creates the Document and History. TODO as each subsystem lands:
 * CheckpointStore, SearchService, MacroLibrary, ViewSettings, DocumentStatistics.
 */
public final class Editor {

    private final Document document = new Document();
    private final History history = new History(document);

    /**
     * Validates, executes and records one change.
     *
     * @param cmd the change
     * @return success with the command's description, or the validation error
     */
    public Result perform(EditCommand cmd) {
        try {
            history.perform(cmd);
            return Result.ok(capitalize(cmd.describe()));
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return what was undone, or "Nothing to undo" */
    public Result undo() {
        try {
            return Result.ok(history.undo());
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return what was redone, or "Nothing to redo" */
    public Result redo() {
        try {
            return Result.ok(history.redo());
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return the tree with section paths and paragraph numbers */
    public Result outline() {
        return Result.ok(document.outline());
    }

    /** @return the document */
    public Document document() {
        return document;
    }

    /** @return the undo/redo history */
    public History history() {
        return history;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
