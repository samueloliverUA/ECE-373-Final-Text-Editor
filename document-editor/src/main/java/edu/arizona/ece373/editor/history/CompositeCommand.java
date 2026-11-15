package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A list of {@link EditCommand}s that acts as one (Composite applied to commands).
 *
 * <p>Replace All builds one of these with one {@code ReplaceTextCommand} part per match, and
 * {@code History.beginGroup} collects a macro replay into one. History treats the whole list as a
 * single entry, so one undo reverses every part (6.8.4, 6.8.7).
 *
 * <p>Parts run in the order they were added and are undone in reverse order. Callers that build a
 * composite whose parts touch the same paragraph (Replace All) add them from the last match to the
 * first, so a part never shifts the offsets of a part that runs after it.
 */

public class CompositeCommand implements EditCommand {
    private final String name;
    private final List<EditCommand> parts = new ArrayList<>();

    /**
     * @param name short name for messages, examples like {@code replace all} or {@code macro tidy}
     */

    public CompositeCommand(String name) {

        this.name = Objects.requireNonNull(name, "name");

    }

    /**
     * Appends a part. It will run after every part added before it.
     *
     * @param c the part
     * @throws IllegalArgumentException if {@code c} is null or this composite itself
     */

    public void add(EditCommand c) {

        if (c == null) {
            throw new IllegalArgumentException("part must not be null");
        }
        if (c == this) {
            throw new IllegalArgumentException("a composite cannot contain itself");
        }
        parts.add(c);
    }

    /**
     * @return the parts in execution order, read only
     */
    public List<EditCommand> parts() {
        return Collections.unmodifiableList(parts);
    }

    /**
     * @return the number of parts
     */
    public int size() {
        return parts.size();
    }

    /**
     * @return true if no part has been added (History skips an empty group)
     */
    public boolean isEmpty() {
        return parts.isEmpty();
    }

    /**
     * Validates every part, in order, before anything is changed. The first failure is thrown and
     * nothing is executed, so a failed Replace All never partly modifies the document (6.1.4).
     */
    @Override
    public void validate(Document d) {
        for (int i = 0; i < parts.size(); i++) {
            EditCommand part = parts.get(i);
            part.validate(d);
        }
    }

    /**
     * Executes every part in order. Called after {@link #validate} succeeded, and again on redo.
     *
     * <p>Safety net: if a part still fails, the parts already executed are undone in reverse order
     * before the exception is rethrown, so the document is never left half-changed.
     */
    @Override
    public void execute(Document d) {
        int done = 0;
        try {
            for (int i = 0; i < parts.size(); i++) {
                EditCommand part = parts.get(i);
                part.execute(d);
                done++;
            }
        } catch (RuntimeException e) {
            for (int i = done - 1; i >= 0; i--) {
                parts.get(i).undo(d);
            }
            throw e;
        }
    }

    /**
     * Undoes every part in reverse order, so each part sees the document it left behind.
     */
    @Override
    public void undo(Document d) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            parts.get(i).undo(d);
        }
    }

    /**
     * @return e.g. {@code replace all (6)}, used in messages such as {@code Undid: replace all (6)}
     */
    @Override
    public String describe() {
        return name + " (" + parts.size() + ")";
    }
}

