package edu.arizona.ece373.editor.history;

import edu.arizona.ece373.editor.model.Document;

/**
 * One undoable change to the document (Command pattern).
 *
 * <p>Rule for every implementation: all checks that can fail go in {@link #validate};
 * {@link #execute} only does work already known to be valid (design doc Section 5.7), so a
 * failed command never partly modifies the document (6.1.4).
 */
public interface EditCommand {

    /**
     * Checks that the change can be made, without changing anything.
     *
     * @param d the document
     * @throws edu.arizona.ece373.editor.model.EditException naming the problem
     */
    void validate(Document d);

    /**
     * Makes the change. Called only after {@link #validate} succeeded (or on redo).
     *
     * @param d the document
     */
    void execute(Document d);

    /**
     * Reverses the most recent {@link #execute}.
     *
     * @param d the document
     */
    void undo(Document d);

    /**
     * Describes this change in a few words for user messages.
     *
     * @return a short description for messages, e.g. {@code added section 2.1}
     */
    String describe();
}
