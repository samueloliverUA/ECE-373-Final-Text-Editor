package edu.arizona.ece373.editor.app;

import edu.arizona.ece373.editor.history.CheckpointStore;
import edu.arizona.ece373.editor.history.EditCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.history.RestoreCheckpointCommand;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.view.ViewSettings;
import java.util.List;

/**
 * The single entry point into the engine (Facade) and the composition root. Every public
 * method that can fail returns a {@link Result}; no exception escapes to the console layer.
 *
 * <p>Owns the Document, History, CheckpointStore and ViewSettings.
 *
 * <p>TODO as each subsystem lands: SearchService ({@code find}, {@code replaceFirst},
 * {@code replaceAll}), MacroLibrary ({@code macros()}), DocumentStatistics ({@code stats()}).
 */
public final class Editor {

    private final Document document = new Document();
    private final History history = new History(document);
    private final CheckpointStore checkpoints = new CheckpointStore();
    private final ViewSettings view = new ViewSettings();

    // ---------------------------------------------------------------------------------------
    // Editing and history
    // ---------------------------------------------------------------------------------------

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
        } catch (EditException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return what was redone, or "Nothing to redo" */
    public Result redo() {
        try {
            return Result.ok(history.redo());
        } catch (EditException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------
    // Checkpoints (UC2)
    // ---------------------------------------------------------------------------------------

    /**
     * Saves the document under a name, overwriting any checkpoint with that name. Not an
     * edit, so not undoable.
     *
     * @param name the checkpoint name
     * @return confirmation, or an error for a blank name
     */
    public Result saveCheckpoint(String name) {
        try {
            checkpoints.save(name, document);
            return Result.ok("Saved checkpoint \"" + name + "\"");
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * Replaces the document with a saved checkpoint. Undoable (UC2 4b).
     *
     * @param name the checkpoint name
     * @return confirmation, or an error naming the saved checkpoints
     */
    public Result restoreCheckpoint(String name) {
        return perform(new RestoreCheckpointCommand(checkpoints, name));
    }

    /** @return the saved checkpoint names, sorted, or "No checkpoints saved" */
    public Result listCheckpoints() {
        List<String> names = checkpoints.names();
        if (names.isEmpty()) {
            return Result.ok("No checkpoints saved");
        }
        return Result.ok("Checkpoints: " + String.join(", ", names));
    }

    // ---------------------------------------------------------------------------------------
    // Viewing (UC4). View changes are not undoable (team exclusion).
    // ---------------------------------------------------------------------------------------

    /**
     * @param mode {@code plain} or {@code markup}
     * @return confirmation, or an error leaving the view unchanged
     */
    public Result setView(String mode) {
        try {
            view.setView(mode);
            return Result.ok("View: " + mode);
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param width the new line width, 10 to 200
     * @return confirmation, or an error leaving the old width (UC4 1a)
     */
    public Result setWidth(int width) {
        try {
            view.setWidth(width);
            return Result.ok("Width: " + width);
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param kind {@code greedy} or {@code balanced}
     * @return confirmation, or an error leaving the breaker unchanged
     */
    public Result setBreaker(String kind) {
        try {
            view.setBreaker(kind);
            return Result.ok("Line breaker: " + kind);
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return the document rendered with the current view, breaker and width */
    public String render() {
        return view.render(document);
    }

    /** @return {@link #render()} as a successful result, for the {@code show} command */
    public Result show() {
        return Result.ok(render());
    }

    /** @return the tree with section paths and paragraph numbers */
    public Result outline() {
        return Result.ok(document.outline());
    }

    // ---------------------------------------------------------------------------------------
    // Collaborators
    // ---------------------------------------------------------------------------------------

    /** @return the document */
    public Document document() {
        return document;
    }

    /** @return the undo/redo history */
    public History history() {
        return history;
    }

    /** @return the named checkpoints */
    public CheckpointStore checkpoints() {
        return checkpoints;
    }

    /** @return the current view, breaker and width */
    public ViewSettings view() {
        return view;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
