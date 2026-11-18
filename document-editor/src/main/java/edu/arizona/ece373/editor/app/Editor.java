package edu.arizona.ece373.editor.app;

import edu.arizona.ece373.editor.history.CheckpointStore;
import edu.arizona.ece373.editor.history.CompositeCommand;
import edu.arizona.ece373.editor.history.EditCommand;
import edu.arizona.ece373.editor.history.History;
import edu.arizona.ece373.editor.history.RestoreCheckpointCommand;
import edu.arizona.ece373.editor.model.Document;
import edu.arizona.ece373.editor.model.EditException;
import edu.arizona.ece373.editor.search.Match;
import edu.arizona.ece373.editor.search.MatchStrategy;
import edu.arizona.ece373.editor.search.SearchService;
import edu.arizona.ece373.editor.view.DocumentStatistics;
import edu.arizona.ece373.editor.view.Statistics;
import edu.arizona.ece373.editor.view.ViewSettings;
import java.util.ArrayList;
import java.util.List;

/**
 * The single entry point into the engine (Facade) and the composition root. Every public
 * method that can fail returns a {@link Result}; no exception escapes to the console layer.
 *
 * <p>Owns the Document, History, CheckpointStore, SearchService, MacroLibrary, ViewSettings
 * and DocumentStatistics (registered as a listener on the document, so its counts go stale
 * on every change).
 */
public final class Editor {

    private final Document document = new Document();
    private final History history = new History(document);
    private final CheckpointStore checkpoints = new CheckpointStore();
    private final SearchService search = new SearchService();
    private final MacroLibrary macros = new MacroLibrary();
    private final ViewSettings view = new ViewSettings();
    private final DocumentStatistics statistics = new DocumentStatistics(document);

    /** Creates an editor with an empty document. */
    public Editor() {
        document.addListener(statistics);
    }

    // ---------------------------------------------------------------------------------------
    // Running commands
    // ---------------------------------------------------------------------------------------

    /**
     * Runs one parsed command the way the console and scripts do: if it succeeds and a macro
     * is being recorded, it is added to that macro (unless it is not recordable). Never
     * throws.
     *
     * @param a the action
     * @return its result
     */
    public Result run(Action a) {
        Result r;
        try {
            r = a.run(this);
        } catch (EditException | IllegalStateException e) {
            return Result.error(e.getMessage());
        }
        if (r.ok) {
            macros.record(a);
        }
        return r;
    }

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

    /** @return the current word, character and paragraph counts */
    public Statistics stats() {
        return statistics.current();
    }

    // ---------------------------------------------------------------------------------------
    // Search and replace (UC5). Only paragraph text is searched, not titles.
    // ---------------------------------------------------------------------------------------

    /**
     * @param query the text to look for
     * @param strategy substring or whole-word matching
     * @return the matches as ranges (e.g. {@code Found 2: p1:0-3, p2:4-7}), "No matches",
     *     or an error for an empty query
     */
    public Result find(String query, MatchStrategy strategy) {
        try {
            List<Match> matches = search.findAll(document, query, strategy);
            if (matches.isEmpty()) {
                return Result.ok(noMatches(query));
            }
            List<String> ranges = new ArrayList<>();
            for (Match m : matches) {
                ranges.add(m.toString());
            }
            return Result.ok("Found " + matches.size() + ": " + String.join(", ", ranges));
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * Replaces the first match, as one undoable change.
     *
     * @param query the text to look for
     * @param replacement the new text
     * @param strategy substring or whole-word matching
     * @return what was replaced, "No matches" (nothing recorded), or an error
     */
    public Result replaceFirst(String query, String replacement, MatchStrategy strategy) {
        try {
            List<Match> matches = search.findAll(document, query, strategy);
            if (matches.isEmpty()) {
                return Result.ok(noMatches(query) + "; nothing replaced");
            }
            return perform(search.replaceFirstCommand(matches.get(0), replacement));
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * Replaces every match as ONE undoable change, so a single undo restores them all.
     *
     * @param query the text to look for
     * @param replacement the new text
     * @param strategy substring or whole-word matching
     * @return how many were replaced, "No matches" (nothing recorded), or an error
     */
    public Result replaceAll(String query, String replacement, MatchStrategy strategy) {
        try {
            List<Match> matches = search.findAll(document, query, strategy);
            CompositeCommand all = search.replaceAllCommand(matches, replacement);
            if (all.isEmpty()) {
                return Result.ok(noMatches(query) + "; nothing replaced");
            }
            return perform(all);
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    private static String noMatches(String query) {
        return "No matches for \"" + query + "\"";
    }

    // ---------------------------------------------------------------------------------------
    // Macros (UC6). These commands are never recorded themselves.
    // ---------------------------------------------------------------------------------------

    /**
     * @param name the macro to record
     * @return confirmation, or an error if already recording
     */
    public Result startMacro(String name) {
        try {
            macros.startRecording(name);
            return Result.ok("Recording macro \"" + name + "\"; 'macro end' to finish");
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /** @return confirmation with the step count, or an error if not recording */
    public Result endMacro() {
        try {
            String name = macros.stopRecording();
            int steps = macros.size(name);
            return Result.ok("Recorded macro \"" + name + "\" (" + steps
                    + (steps == 1 ? " step)" : " steps)"));
        } catch (EditException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * @param name the macro to replay as one undoable change
     * @return confirmation, or an error naming the failing step (replay rolled back)
     */
    public Result playMacro(String name) {
        return macros.play(name, this);
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

    /** @return the recorded macros */
    public MacroLibrary macros() {
        return macros;
    }

    /** @return the search service */
    public SearchService search() {
        return search;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
