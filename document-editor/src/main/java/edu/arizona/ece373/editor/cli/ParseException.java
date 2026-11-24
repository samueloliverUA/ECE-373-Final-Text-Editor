package edu.arizona.ece373.editor.cli;

/**
 * Thrown by {@link CommandParser} and {@link Tokens} when a command line is not valid syntax.
 * Checked, so ConsoleApp and ScriptRunner must handle it. The message is shown to the user
 * as-is and is always one readable line.
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a user-facing message.
     *
     * @param message one line naming the problem
     */
    public ParseException(String message) {
        super(message);
    }
}
