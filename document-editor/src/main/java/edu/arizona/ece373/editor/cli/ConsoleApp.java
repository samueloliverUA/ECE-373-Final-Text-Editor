package edu.arizona.ece373.editor.cli;

import edu.arizona.ece373.editor.app.Action;
import edu.arizona.ece373.editor.app.Editor;
import edu.arizona.ece373.editor.app.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Program entry point. Runs the interactive prompt, or a script passed on the command line
 * ({@code java -jar editor.jar demo.txt}).
 *
 * <p>{@link #main} builds the one {@link Editor}, {@link CommandParser} and
 * {@link ScriptRunner}; nothing else creates them. All output goes through the
 * {@link PrintStream} given to the constructor, so tests can capture it. Bad input never
 * crashes the program or prints a stack trace (6.1.4).
 *
 * <p>Satisfies 6.1.1, 6.1.2, 6.1.4.
 */
public final class ConsoleApp {

    /** Shown before each interactive command. */
    public static final String PROMPT = "> ";

    private final Editor editor;
    private final CommandParser parser;
    private final ScriptRunner scripts;
    private final PrintStream out;

    /**
     * @param editor the editor
     * @param parser the command parser
     * @param scripts the script runner (sharing the same parser and editor)
     * @param out where all output goes
     */
    public ConsoleApp(Editor editor, CommandParser parser, ScriptRunner scripts,
            PrintStream out) {
        this.editor = editor;
        this.parser = parser;
        this.scripts = scripts;
        this.out = out;
    }

    /**
     * Starts the editor.
     *
     * @param args empty for an interactive session, or one script file to run
     */
    public static void main(String[] args) {
        Editor editor = new Editor();
        CommandParser parser = new CommandParser();
        ScriptRunner scripts = new ScriptRunner(parser, editor);
        ConsoleApp app = new ConsoleApp(editor, parser, scripts, System.out);
        int status = args.length > 0 ? app.runScript(Path.of(args[0])) : app.interactive(System.in);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Runs a script file, then returns.
     *
     * @param script the file
     * @return 0 if the file was read, 1 if it could not be
     */
    public int runScript(Path script) {
        Result result = scripts.run(script, out);
        if (!result.ok) {
            out.println("Error: " + result.message);
            return 1;
        }
        if (!result.quit) {
            out.println(result.message);
        }
        return 0;
    }

    /**
     * Interactive loop: prompt, read one line, parse, run, print the result. Ends on
     * {@code quit} or end of input.
     *
     * @param in where commands are read from
     * @return 0
     */
    public int interactive(InputStream in) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        out.println("Document editor. Type commands; 'quit' to exit.");
        while (true) {
            out.print(PROMPT);
            out.flush();
            String line;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                out.println("Error: could not read input: " + e.getMessage());
                return 0;
            }
            if (line == null) {
                out.println();
                return 0;
            }
            if (line.isBlank()) {
                continue;
            }
            Result result = execute(line);
            out.println(result.ok ? result.message : "Error: " + result.message);
            if (result.quit) {
                return 0;
            }
        }
    }

    /**
     * Parses and runs one line. Never throws.
     *
     * @param line the command text
     * @return the result to show
     */
    public Result execute(String line) {
        try {
            Action action = parser.parse(line);
            return editor.run(action); // records it if a macro is being recorded
        } catch (ParseException e) {
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            // Last-resort guard: report instead of crashing with a stack trace.
            return Result.error("internal error: " + e);
        }
    }
}
