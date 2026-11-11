package edu.arizona.ece373.editor.app;

/**
 * One parsed command, ready to run against the {@link Editor} (Command pattern, UI level;
 * {@code EditCommand} is the document-level one). Macros store and replay these (6.8.5).
 */
@FunctionalInterface
public interface Action {

    /**
     * Runs this command.
     *
     * @param e the editor
     * @return the outcome to show the user
     */
    Result run(Editor e);

    /**
     * @return false for actions a macro must not record (undo, redo, macro, run, quit;
     *     UC6 alternate flow 1b)
     */
    default boolean recordable() {
        return true;
    }
}
