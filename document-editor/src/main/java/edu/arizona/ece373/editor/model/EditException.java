package edu.arizona.ece373.editor.model;

/**
 * Thrown by {@code EditCommand.validate} and by model lookups when a change cannot be made.
 * The message is shown to the user as-is (for example {@code "No section 2.4"}), and
 * {@code Editor} turns it into {@code Result.error}.
 */
public class EditException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message one readable line naming the problem
     */
    public EditException(String message) {
        super(message);
    }
}
