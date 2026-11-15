/*
 * CompositeCommand.java   (class)
 * Package:   edu.arizona.ece373.editor.history
 * Owner:     Jack        Reviewer: Sam
 * When:      Week of Oct 28
 * Pattern:   Composite + Command
 * Satisfies: 6.8.4, 6.8.7
 * Design doc: Fig 2a, Figs 4, 5, 7; Section 5.3
 *
 * PURPOSE: A list of EditCommands that acts as one: Replace All and macro replays.
 *
 * MEMBERS (from the class diagram):
 *   - -parts : List<EditCommand>
 *   - +add(c : EditCommand)
 *
 * WHAT TO DO:
 *   1. Implement EditCommand. add(c) appends a part.
 *   2. validate each part; execute parts in order; undo parts in REVERSE order.
 *
 * TESTED BY: HistoryTest, ReplaceAllTest, MacroLibraryTest
 *
 * (Placeholder only - no code yet. Replace this comment with the class and its JavaDoc.)
 */

package edu.arizona.edu373.editor.history;

import edu.arizona.edu.ece373.editor.model.Document;
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

public class CompositeCommand implements EditCommand{
    private final String name;
    private final List<EditCommand> parts = new ArrayList<>();

    /**
     * @param name short name for messages, examples like {@code replace all} or {@code macro tidy}
     */

    public CompositeCommand(String name){

        this.name=Objects.requireNonNull(name,"name");

    }

    /**
     * Appends a part. It will run after every part added before it.
     *
     * @param c the part
     * @throws IllegalArgumentException if {@code c} is null or this composite itself
     */

    public void add(EditCommand c){

        if(c==null){
            throw new IllegalArgumentException("part must not be null");
        }
        if(n==this) {
            throw new IllegalArgumentException("a composite cannot contain itself");
        }
        parts.add(c);
        }

    /** @return the parts in execution order, read only */
    public List<EditCommand> parts() {
        return Collections.unmodifiableList(parts);
    }

    /** @return the number of parts */
    public int size() {
        return parts.size();
    }

    /** @return true if no part has been added (History skips an empty group) */
    public boolean isEmpty() {
        return parts.isEmpty();
    }

}

