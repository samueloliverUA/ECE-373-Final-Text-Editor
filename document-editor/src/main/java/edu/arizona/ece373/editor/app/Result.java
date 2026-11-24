package edu.arizona.ece373.editor.app;

/**
 * Immutable outcome of a command: success flag plus the message shown to the user.
 */
public final class Result {

    /** True if the command succeeded. */
    public final boolean ok;
    /** The message shown to the user. */
    public final String message;
    /** True if the console should exit after showing this result. */
    public final boolean quit;

    private Result(boolean ok, String message, boolean quit) {
        this.ok = ok;
        this.message = message;
        this.quit = quit;
    }

    /**
     * Creates a success result.
     *
     * @param msg the message to show
     * @return a success result
     */
    public static Result ok(String msg) {
        return new Result(true, msg, false);
    }

    /**
     * Creates a failure result.
     *
     * @param msg one line naming the problem
     * @return a failure result
     */
    public static Result error(String msg) {
        return new Result(false, msg, false);
    }

    /**
     * Creates a result that ends the program.
     *
     * @param msg the goodbye message
     * @return a success result that tells the console to exit
     */
    public static Result quit(String msg) {
        return new Result(true, msg, true);
    }

    @Override
    public String toString() {
        return (ok ? "" : "Error: ") + message;
    }
}
